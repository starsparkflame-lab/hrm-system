package com.cow.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Attendance;
import com.cow.entity.Dept;
import com.cow.entity.DeptWorkCalendar;
import com.cow.entity.Staff;
import com.cow.enums.AttendanceStatusEnum;
import com.cow.mapper.AttendanceMapper;
import com.cow.mapper.DeptMapper;
import com.cow.mapper.DeptWorkCalendarMapper;
import com.cow.mapper.StaffMapper;
import com.cow.util.DatetimeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上班日历服务类
 */
@Service
public class CalendarService {

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private AttendanceMapper attendanceMapper;

    @Resource
    private DeptWorkCalendarMapper deptWorkCalendarMapper;

    /**
     * 获取部门考勤日历
     * @param deptId 部门ID(管理员可不传或传0查看所有部门,普通员工不传则自动获取自己的部门)
     * @param month 月份(格式:yyyyMM)
     * @param currentStaffDeptId 当前登录员工的部门ID
     * @param currentStaffRole 当前登录员工的角色代码
     * @return 部门考勤日历数据
     */
    public ResponseDTO getDeptCalendar(Integer deptId, String month, Integer currentStaffDeptId, String currentStaffRole) {
        // 默认当前月份
        if (month == null || month.isEmpty()) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }

        // 获取月份的所有日期
        String[] monthDayList = DatetimeUtil.getMonthDayList(month);

        // 查询部门列表
        List<Dept> deptList;
        
        // 判断是否为管理员
        System.out.println("[DEBUG Service] currentStaffRole=" + currentStaffRole + ", isAdmin=" + "admin".equals(currentStaffRole));
        boolean isAdmin = "admin".equals(currentStaffRole);
        
        if (isAdmin) {
            // 管理员:可以查看所有部门或指定部门
            if (deptId != null && deptId > 0) {
                // 查询指定部门
                Dept dept = deptMapper.selectById(deptId);
                if (dept != null) {
                    deptList = new ArrayList<>();
                    deptList.add(dept);
                } else {
                    deptList = new ArrayList<>();
                }
            } else {
                // 查询所有子部门(不含顶级部门)
                deptList = deptMapper.selectList(new QueryWrapper<Dept>().ne("parent_id", 0));
                System.out.println("[DEBUG] 管理员查询所有部门，共找到 " + deptList.size() + " 个子部门");
                deptList.forEach(d -> System.out.println("[DEBUG] 部门: " + d.getName() + " (ID=" + d.getId() + ")"));
            }
        } else {
            // 普通员工:只能查看自己部门的考勤日历
            if (currentStaffDeptId != null && currentStaffDeptId > 0) {
                Dept dept = deptMapper.selectById(currentStaffDeptId);
                if (dept != null) {
                    deptList = new ArrayList<>();
                    deptList.add(dept);
                } else {
                    deptList = new ArrayList<>();
                }
            } else {
                // 员工没有部门,返回空列表
                deptList = new ArrayList<>();
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();

        System.out.println("[DEBUG] 开始处理 " + deptList.size() + " 个部门");
        
        // 遍历每个部门
        for (Dept dept : deptList) {
            System.out.println("[DEBUG] 处理部门: " + dept.getName() + " (ID=" + dept.getId() + ")");
            
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("deptId", dept.getId());
            deptMap.put("deptName", dept.getName());

            // 查询该部门的所有员工
            List<Staff> staffList = staffMapper.selectList(
                new QueryWrapper<Staff>().eq("dept_id", dept.getId())
            );
            System.out.println("[DEBUG] 部门 " + dept.getName() + " 有 " + staffList.size() + " 个员工");

            // 查询该部门的日历设置
            QueryWrapper<DeptWorkCalendar> calendarQuery = new QueryWrapper<>();
            String startDate = monthDayList[0].substring(0, 4) + "-" + monthDayList[0].substring(4, 6) + "-01";
            String endDate = monthDayList[monthDayList.length - 1].substring(0, 4) + "-" + 
                           monthDayList[monthDayList.length - 1].substring(4, 6) + "-31";
            calendarQuery.eq("dept_id", dept.getId())
                        .ge("work_date", startDate)
                        .le("work_date", endDate);
            List<DeptWorkCalendar> deptCalendars = deptWorkCalendarMapper.selectList(calendarQuery);

            // 统计整月的考勤数据 - 完全基于部门日历表
            int totalNormalCount = 0;
            int totalLateCount = 0;
            int totalLeaveEarlyCount = 0;
            int totalAbsenteeismCount = 0;
            int totalLeaveCount = 0;

            // 统计每天的考勤情况
            List<Map<String, Object>> calendarData = new ArrayList<>();
            for (String day : monthDayList) {
                Map<String, Object> dayMap = new HashMap<>();
                Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                dayMap.put("date", date);

                // 解析日期
                LocalDate localDate = LocalDate.parse(day.substring(0, 4) + "-" + 
                                                      day.substring(4, 6) + "-" + 
                                                      day.substring(6, 8));
                
                // 检查部门日历设置
                DeptWorkCalendar deptCalendar = deptCalendars.stream()
                    .filter(c -> c.getWorkDate().equals(localDate))
                    .findFirst()
                    .orElse(null);

                // 如果部门日历有设置,使用部门日历的设置
                if (deptCalendar != null) {
                    if (deptCalendar.getIsWorkday() == 0) {
                        // 设置为休假
                        String remark = deptCalendar.getRemark();
                        if (remark == null || remark.trim().isEmpty()) {
                            remark = "休假";
                        }
                        dayMap.put("status", "holiday");
                        dayMap.put("message", remark);
                        dayMap.put("tagType", "info");
                        dayMap.put("totalCount", staffList.size());
                        dayMap.put("normalCount", 0);
                        dayMap.put("lateCount", 0);
                        dayMap.put("leaveEarlyCount", 0);
                        dayMap.put("absenteeismCount", 0);
                        dayMap.put("leaveCount", staffList.size());
                        
                        // 累加月度统计
                        totalLeaveCount += staffList.size();
                    } else {
                        // 设置为上班,根据预期状态进行统计
                        dayMap.put("status", "workday");
                        
                        Integer expectedStatus = deptCalendar.getExpectedStatus();
                        if (expectedStatus == null) {
                            expectedStatus = 1; // 默认为正常
                        }
                        
                        dayMap.put("totalCount", staffList.size());
                        
                        // 根据预期状态设置显示和统计
                        switch (expectedStatus) {
                            case 1: // 正常
                                dayMap.put("message", "上班");
                                dayMap.put("tagType", "success");
                                dayMap.put("normalCount", staffList.size());
                                dayMap.put("lateCount", 0);
                                dayMap.put("leaveEarlyCount", 0);
                                dayMap.put("absenteeismCount", 0);
                                dayMap.put("leaveCount", 0);
                                totalNormalCount += staffList.size();
                                break;
                            case 2: // 迟到
                                dayMap.put("message", "迟到");
                                dayMap.put("tagType", "warning");
                                dayMap.put("normalCount", 0);
                                dayMap.put("lateCount", staffList.size());
                                dayMap.put("leaveEarlyCount", 0);
                                dayMap.put("absenteeismCount", 0);
                                dayMap.put("leaveCount", 0);
                                totalLateCount += staffList.size();
                                break;
                            case 3: // 早退
                                dayMap.put("message", "早退");
                                dayMap.put("tagType", "warning");
                                dayMap.put("normalCount", 0);
                                dayMap.put("lateCount", 0);
                                dayMap.put("leaveEarlyCount", staffList.size());
                                dayMap.put("absenteeismCount", 0);
                                dayMap.put("leaveCount", 0);
                                totalLeaveEarlyCount += staffList.size();
                                break;
                            case 4: // 缺勤
                                dayMap.put("message", "缺勤");
                                dayMap.put("tagType", "danger");
                                dayMap.put("normalCount", 0);
                                dayMap.put("lateCount", 0);
                                dayMap.put("leaveEarlyCount", 0);
                                dayMap.put("absenteeismCount", staffList.size());
                                dayMap.put("leaveCount", 0);
                                totalAbsenteeismCount += staffList.size();
                                break;
                            default: // 默认为正常
                                dayMap.put("message", "上班");
                                dayMap.put("tagType", "success");
                                dayMap.put("normalCount", staffList.size());
                                dayMap.put("lateCount", 0);
                                dayMap.put("leaveEarlyCount", 0);
                                dayMap.put("absenteeismCount", 0);
                                dayMap.put("leaveCount", 0);
                                totalNormalCount += staffList.size();
                                break;
                        }
                    }
                    calendarData.add(dayMap);
                    continue;
                }

                // 如果没有部门日历设置,默认工作日上班,周末休假
                if (DateUtil.isWeekend(date)) {
                    dayMap.put("status", "weekend");
                    dayMap.put("message", "休假");
                    dayMap.put("tagType", "info");
                    dayMap.put("totalCount", staffList.size());
                    dayMap.put("normalCount", 0);
                    dayMap.put("lateCount", 0);
                    dayMap.put("leaveEarlyCount", 0);
                    dayMap.put("absenteeismCount", 0);
                    dayMap.put("leaveCount", staffList.size());
                    
                    // 累加月度统计
                    totalLeaveCount += staffList.size();
                } else {
                    // 工作日默认上班
                    dayMap.put("status", "workday");
                    dayMap.put("message", "上班");
                    dayMap.put("tagType", "success");
                    dayMap.put("totalCount", staffList.size());
                    dayMap.put("normalCount", 0);
                    dayMap.put("lateCount", 0);
                    dayMap.put("leaveEarlyCount", 0);
                    dayMap.put("absenteeismCount", 0);
                    dayMap.put("leaveCount", 0);
                    
                    // 累加月度统计
                    totalNormalCount += staffList.size();
                }

                calendarData.add(dayMap);
            }

            // 添加部门的月度统计信息
            deptMap.put("normalCount", totalNormalCount);
            deptMap.put("lateCount", totalLateCount);
            deptMap.put("leaveEarlyCount", totalLeaveEarlyCount);
            deptMap.put("absenteeismCount", totalAbsenteeismCount);
            deptMap.put("leaveCount", totalLeaveCount);

            deptMap.put("calendarData", calendarData);
            resultList.add(deptMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("deptList", resultList);

        return Response.success(result);
    }
}
