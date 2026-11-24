package com.cow.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Attendance;
import com.cow.entity.Checkin;
import com.cow.entity.Dept;
import com.cow.enums.AttendanceStatusEnum;
import com.cow.mapper.CheckinMapper;
import com.cow.mapper.AttendanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤签到Service
 */
@Service
public class CheckinService extends ServiceImpl<CheckinMapper, Checkin> {

    @Resource
    private CheckinMapper checkinMapper;

    @Resource
    private AttendanceMapper attendanceMapper;

    /**
     * 部门作息时间走缓存读取，避免每次签到都关联查询 sys_dept
     */
    @Resource
    private DeptService deptService;

    /**
     * 员工签到
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO doCheckin(Integer staffId, Integer checkinType, String location) {
        // 获取当前日期和时间
        Date today = new Date(System.currentTimeMillis());
        LocalTime now = LocalTime.now().withNano(0); // 去掉毫秒
        Time currentTime = Time.valueOf(now.toString());

        // 检查是否已签到
        int count = checkinMapper.countCheckin(staffId, today, checkinType);
        if (count > 0) {
            return Response.error("今日该时段已签到,请勿重复签到");
        }

        // 获取员工部门信息，验证签到时间
        Dept dept = deptService.findDeptByStaffIdWithCache(staffId);
        if (dept == null) {
            return Response.error("未找到员工部门信息");
        }

        // 验证签到时间是否在规定范围内
        String timeCheckResult = validateCheckinTime(checkinType, now, dept);
        if (timeCheckResult != null) {
            return Response.error(timeCheckResult);
        }

        // 插入签到记录
        int result = checkinMapper.insertCheckin(staffId, today, currentTime, checkinType, location);
        if (result > 0) {
            // 同步到考勤表现
            syncToAttendance(staffId, today, checkinType, currentTime);
            
            Map<String, Object> data = new HashMap<>();
            data.put("checkinTime", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            data.put("checkinType", checkinType);
            return Response.success("签到成功", data);
        }
        return Response.error("签到失败");
    }

    /**
     * 验证签到时间是否在规定范围内
     * @return null表示验证通过，否则返回错误信息
     */
    private String validateCheckinTime(Integer checkinType, LocalTime now, Dept dept) {
        switch (checkinType) {
            case 1: // 上午上班签到
                if (dept.getMorStartTime() == null) {
                    return "部门未设置上午上班时间";
                }
                LocalTime morStart = dept.getMorStartTime().toLocalDateTime().toLocalTime();
                LocalTime morStartEarly = morStart.minusMinutes(30); // 上班前30分钟
                LocalTime morStartLate = morStart.plusMinutes(10);   // 上班后10分钟
                
                if (now.isBefore(morStartEarly) || now.isAfter(morStartLate)) {
                    return String.format("上午上班签到时间：%s ~ %s，当前时间：%s",
                        morStartEarly, morStartLate, now);
                }
                break;
                
            case 2: // 上午下班签退
                if (dept.getMorEndTime() == null) {
                    return "部门未设置上午下班时间";
                }
                LocalTime morEnd = dept.getMorEndTime().toLocalDateTime().toLocalTime();
                LocalTime morEndEarly = morEnd.minusMinutes(5);  // 下班前5分钟
                LocalTime morEndLate = morEnd.plusMinutes(30);   // 下班后30分钟
                
                if (now.isBefore(morEndEarly) || now.isAfter(morEndLate)) {
                    return String.format("上午下班签退时间：%s ~ %s，当前时间：%s",
                        morEndEarly, morEndLate, now);
                }
                break;
                
            case 3: // 下午上班签到
                if (dept.getAftStartTime() == null) {
                    return "部门未设置下午上班时间";
                }
                LocalTime aftStart = dept.getAftStartTime().toLocalDateTime().toLocalTime();
                LocalTime aftStartEarly = aftStart.minusMinutes(30); // 上班前30分钟
                LocalTime aftStartLate = aftStart.plusMinutes(10);   // 上班后10分钟
                
                if (now.isBefore(aftStartEarly) || now.isAfter(aftStartLate)) {
                    return String.format("下午上班签到时间：%s ~ %s，当前时间：%s",
                        aftStartEarly, aftStartLate, now);
                }
                break;
                
            case 4: // 下午下班签退
                if (dept.getAftEndTime() == null) {
                    return "部门未设置下午下班时间";
                }
                LocalTime aftEnd = dept.getAftEndTime().toLocalDateTime().toLocalTime();
                LocalTime aftEndEarly = aftEnd.minusMinutes(5);  // 下班前5分钟
                LocalTime aftEndLate = aftEnd.plusMinutes(30);   // 下班后30分钟
                
                if (now.isBefore(aftEndEarly) || now.isAfter(aftEndLate)) {
                    return String.format("下午下班签退时间：%s ~ %s，当前时间：%s",
                        aftEndEarly, aftEndLate, now);
                }
                break;
        }
        return null; // 验证通过
    }

    /**
     * 查询员工今日签到记录
     */
    public ResponseDTO getTodayCheckins(Integer staffId) {
        Date today = new Date(System.currentTimeMillis());
        List<Checkin> list = checkinMapper.findTodayCheckins(staffId, today);
        return Response.success(list);
    }

    /**
     * 查询签到状态(今日哪些时段已签到)
     */
    public ResponseDTO getCheckinStatus(Integer staffId) {
        Date today = new Date(System.currentTimeMillis());
        Map<String, Object> status = new HashMap<>();
        
        // 查询4个时段的签到状态
        status.put("morningWork", checkinMapper.countCheckin(staffId, today, 1) > 0);
        status.put("morningOff", checkinMapper.countCheckin(staffId, today, 2) > 0);
        status.put("afternoonWork", checkinMapper.countCheckin(staffId, today, 3) > 0);
        status.put("afternoonOff", checkinMapper.countCheckin(staffId, today, 4) > 0);
        
        return Response.success(status);
    }

    /**
     * 获取员工所在部门信息
     */
    public Dept getDeptByStaffId(Integer staffId) {
        return deptService.findDeptByStaffIdWithCache(staffId);
    }

    /**
     * 同步签到记录到考勤表现
     */
    private void syncToAttendance(Integer staffId, Date today, Integer checkinType, Time checkinTime) {
        try {
            // 查找或创建考勤记录
            Attendance attendance = attendanceMapper.findByStaffId(staffId, 
                today.toString().replace("-", ""));
            
            if (attendance == null) {
                // 创建新的考勤记录
                attendance = new Attendance();
                attendance.setStaffId(staffId);
                attendance.setAttendanceDate(today);
                attendance.setStatus(AttendanceStatusEnum.ABSENTEEISM); // 默认旷工(未完成打卡)
            }
            
            // 根据签到类型设置对应的时间字段
            Timestamp timestamp = new Timestamp(checkinTime.getTime());
            switch (checkinType) {
                case 1: // 上午上班
                    attendance.setMorStartTime(timestamp);
                    break;
                case 2: // 上午下班
                    attendance.setMorEndTime(timestamp);
                    break;
                case 3: // 下午上班
                    attendance.setAftStartTime(timestamp);
                    break;
                case 4: // 下午下班
                    attendance.setAftEndTime(timestamp);
                    break;
            }
            
            // 判断考勤状态
            Dept dept = deptService.findDeptByStaffIdWithCache(staffId);
            if (dept != null) {
                // 如果4个打卡都完成了，判断是否迟到、早退或旷工
                if (attendance.getMorStartTime() != null && attendance.getMorEndTime() != null
                        && attendance.getAftStartTime() != null && attendance.getAftEndTime() != null) {
                    if (isAbsenteeism(attendance, dept)) {
                        attendance.setStatus(AttendanceStatusEnum.ABSENTEEISM);
                    } else if (isLate(attendance, dept)) {
                        attendance.setStatus(AttendanceStatusEnum.LATE);
                    } else if (isLeaveEarly(attendance, dept)) {
                        attendance.setStatus(AttendanceStatusEnum.LEAVE_EARLY);
                    } else {
                        attendance.setStatus(AttendanceStatusEnum.NORMAL);
                    }
                } else {
                    // 还未完成全部打卡，保持原有状态(旷工)
                    // 不修改状态，保持ABSENTEEISM
                }
            }
            
            // 保存或更新
            attendanceMapper.insertOrUpdate(attendance);
        } catch (Exception e) {
            // 记录错误但不影响签到
            e.printStackTrace();
        }
    }

    /**
     * 是否迟到
     * 规则: 上班后10分钟内签到算正常，超过10分钟算迟到
     */
    private boolean isLate(Attendance attendance, Dept dept) {
        // 上午上班迟到判断: 超过规定上班时间10分钟
        if (attendance.getMorStartTime() != null && dept.getMorStartTime() != null) {
            LocalTime actualTime = attendance.getMorStartTime().toLocalDateTime().toLocalTime();
            LocalTime deptTime = dept.getMorStartTime().toLocalDateTime().toLocalTime();
            LocalTime lateLimit = deptTime.plusMinutes(10); // 迟到界限: 上班时间+10分钟
            
            if (actualTime.isAfter(lateLimit)) {
                return true; // 超过10分钟，算迟到
            }
        }
        
        // 下午上班迟到判断
        if (attendance.getAftStartTime() != null && dept.getAftStartTime() != null) {
            LocalTime actualTime = attendance.getAftStartTime().toLocalDateTime().toLocalTime();
            LocalTime deptTime = dept.getAftStartTime().toLocalDateTime().toLocalTime();
            LocalTime lateLimit = deptTime.plusMinutes(10);
            
            if (actualTime.isAfter(lateLimit)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 是否早退
     * 规则: 下班前5分钟内签退算正常，提前超过5分钟算早退
     */
    private boolean isLeaveEarly(Attendance attendance, Dept dept) {
        // 上午下班早退判断: 提前超过5分钟
        if (attendance.getMorEndTime() != null && dept.getMorEndTime() != null) {
            LocalTime actualTime = attendance.getMorEndTime().toLocalDateTime().toLocalTime();
            LocalTime deptTime = dept.getMorEndTime().toLocalDateTime().toLocalTime();
            LocalTime earlyLimit = deptTime.minusMinutes(5); // 早退界限: 下班时间-5分钟
            
            if (actualTime.isBefore(earlyLimit)) {
                return true; // 提前超过5分钟，算早退
            }
        }
        
        // 下午下班早退判断
        if (attendance.getAftEndTime() != null && dept.getAftEndTime() != null) {
            LocalTime actualTime = attendance.getAftEndTime().toLocalDateTime().toLocalTime();
            LocalTime deptTime = dept.getAftEndTime().toLocalDateTime().toLocalTime();
            LocalTime earlyLimit = deptTime.minusMinutes(5);
            
            if (actualTime.isBefore(earlyLimit)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 是否旷工
     * 规则: 4个时段有任何一个未打卡就算旷工
     */
    private boolean isAbsenteeism(Attendance attendance, Dept dept) {
        // 如果4个打卡有任何一个没打，视为旷工
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return false;
    }
}
