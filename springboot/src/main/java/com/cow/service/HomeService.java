package com.cow.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Quarter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cow.cache.CacheKeys;
import com.cow.cache.CacheService;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Attendance;
import com.cow.entity.City;
import com.cow.entity.Dept;
import com.cow.entity.Staff;
import com.cow.enums.AttendanceStatusEnum;
import com.cow.mapper.AttendanceMapper;
import com.cow.util.DatetimeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 * @Date 2022/2/27
 * @Version 1.0
 */

@Service
public class HomeService {

    @Resource
    private StaffService staffService;

    @Resource
    private CityService cityService;

    @Resource
    private AttendanceMapper attendanceMapper;

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private DeptService deptService;

    @Resource
    private CacheService cacheService;


    // 统计当前年份入职员工数量
    public ResponseDTO getStaffData() {
        // 首页图表数据，需遍历全量员工做季度聚合，用短 TTL 缓存削峰
        Object cached = this.cacheService.get(CacheKeys.HOME_STAFF);
        if (cached != null) {
            return Response.success(cached);
        }
        QueryWrapper queryWrapper = new QueryWrapper<Attendance>().ge("create_time", DateUtil.thisYear() + "-01-01");
        List<Staff> list = this.staffService.list(queryWrapper);
        int q1 = 0, q2 = 0, q3 = 0, q4 = 0;
        for (Staff staff : list) {
            Quarter quarter = DateUtil.quarterEnum(staff.getCreateTime());
            // 统计每个季度的注册人数
            switch (quarter) {
                case Q1:
                    q1 += 1;
                    break;
                case Q2:
                    q2 += 1;
                    break;
                case Q3:
                    q3 += 1;
                    break;
                case Q4:
                    q4 += 1;
                    break;
                default:
                    break;
            }
        }
        List<Integer> result = CollUtil.newArrayList(q1, q2, q3, q4);
        this.cacheService.set(CacheKeys.HOME_STAFF, result, CacheKeys.HOME_STAT_TTL);
        return Response.success(result);
    }

    /**
     * 获取统计数据
     * @param deptId 部门ID(可选)
     * @param loginType 登录类型(admin=管理员,staff=员工)
     */
    public ResponseDTO getCountData(Integer deptId, String loginType) {
        // 该接口需多次 count 查询，且每个用户进首页都会调用，按可见范围分键缓存
        String cacheKey = CacheKeys.homeCount(deptId, loginType);
        Object cached = this.cacheService.get(cacheKey);
        if (cached != null) {
            return Response.success(cached);
        }

        // 获取总员工数
        long totalNum;
        long normalNum;
        
        // 判断是否为管理员登录
        boolean isAdmin = "admin".equals(loginType);
        
        if (deptId != null && deptId > 0 && !isAdmin) {
            // 普通员工登录且指定了部门ID,只统计该部门的员工
            QueryWrapper<Staff> totalQuery = new QueryWrapper<>();
            totalQuery.eq("dept_id", deptId);
            totalNum = this.staffService.count(totalQuery);
            
            QueryWrapper<Staff> normalQuery = new QueryWrapper<>();
            normalQuery.eq("dept_id", deptId).eq("status", 1);
            normalNum = this.staffService.count(normalQuery);
        } else {
            // 管理员登录或未指定部门,统计全部员工
            totalNum = this.staffService.count();
            normalNum = this.staffService.count(new QueryWrapper<Staff>().eq("status", 1));
        }
        
        // 统计当日考勤数据
        Date datetime = new Date(System.currentTimeMillis());
        String day = DateUtil.format(datetime, "yyyy-MM-dd");
        long lateNum = this.attendanceService.count(new QueryWrapper<Attendance>().eq("attendance_date", day).eq("status", AttendanceStatusEnum.LATE.getCode()));
        long leaveEarlyNum = this.attendanceService.count(new QueryWrapper<Attendance>().eq("attendance_date", day).eq("status", AttendanceStatusEnum.LEAVE_EARLY.getCode()));
        long absenteeismNum = this.attendanceService.count(new QueryWrapper<Attendance>().eq("attendance_date", day).eq("status", AttendanceStatusEnum.ABSENTEEISM.getCode()));
        Map<String, Object> map = new HashMap<>();
        map.put("totalNum", totalNum);
        map.put("normalNum", normalNum);
        map.put("lateNum", lateNum);
        map.put("leaveEarlyNum", leaveEarlyNum);
        map.put("absenteeismNum", absenteeismNum);
        this.cacheService.set(cacheKey, map, CacheKeys.HOME_STAT_TTL);
        return Response.success(map);
    }

    public ResponseDTO getCityData() {
        // 城市社保比例属于近乎静态的基础数据，缓存 1 小时
        Object cached = this.cacheService.get(CacheKeys.HOME_CITY);
        if (cached != null) {
            return Response.success(cached);
        }
        QueryWrapper<City> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("limit 5");
        List<City> list = this.cityService.list(queryWrapper);
        this.cacheService.set(CacheKeys.HOME_CITY, list, CacheKeys.CITY_TTL);
        return Response.success(list);
    }


    /**
     * 统计员工当月的考勤信息
     * @param id 员工id
     * @param month
     * @return
     */
    public ResponseDTO getAttendanceData(Integer id, String month) {
        if (month == null || month == "") {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String[] monthDayList = DatetimeUtil.getMonthDayList(month);
        // 考勤状态表
        List<HashMap<String,Object>> list = new ArrayList<>();
        for (String day : monthDayList) {
            HashMap<String, Object> map = new HashMap<>();
            Attendance attendance = this.attendanceMapper.findByStaffId(id, day);
            if (attendance == null) {
                Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                // 如果是未来日期,不显示状态
                if (date.after(new Date(System.currentTimeMillis()))) {
                    map.put("message", "");
                    map.put("tagType", "");
                } else if (DateUtil.isWeekend(date)) {
                    // 如果是周末就休假
                    map.put("message", AttendanceStatusEnum.LEAVE.getMessage());
                    map.put("tagType",AttendanceStatusEnum.LEAVE.getTagType());
                } else {
                    // 工作日没有考勤记录,显示旷工
                    map.put("message",AttendanceStatusEnum.ABSENTEEISM.getMessage());
                    map.put("tagType",AttendanceStatusEnum.ABSENTEEISM.getTagType());
                }
            } else {
                map.put("message",attendance.getStatus().getMessage());
                map.put("tagType",attendance.getStatus().getTagType());
            }
            list.add(map);
        }
        return Response.success(list);
    }


    // 统计各部门的人数情况
    public ResponseDTO getDepartmentData() {
        // 需逐一级部门循环统计，用短 TTL 缓存削峰
        Object cached = this.cacheService.get(CacheKeys.HOME_DEPARTMENT);
        if (cached != null) {
            return Response.success(cached);
        }
        List<Dept> parentList = this.deptService.list(new QueryWrapper<Dept>().eq("parent_id", 0));
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Dept parentDept : parentList) {
            List<Dept> list = this.deptService.list(new QueryWrapper<Dept>().eq("parent_id", parentDept.getId()));
            List<Integer> ids = list.stream().map(Dept::getId).collect(Collectors.toList());
            long num = this.staffService.count(new QueryWrapper<Staff>().in("dept_id", ids));
            Map<String, Object> map = new HashMap<>();
            map.put("value", num);
            map.put("name", parentDept.getName());
            mapList.add(map);
        }
        this.cacheService.set(CacheKeys.HOME_DEPARTMENT, mapList, CacheKeys.HOME_STAT_TTL);
        return Response.success(mapList);
    }
}
