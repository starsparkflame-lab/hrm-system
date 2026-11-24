package com.cow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.DeptWorkCalendar;
import com.cow.mapper.DeptWorkCalendarMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门上班日历服务类
 */
@Service
public class DeptWorkCalendarService extends ServiceImpl<DeptWorkCalendarMapper, DeptWorkCalendar> {

    /**
     * 批量保存部门上班日历
     */
    public ResponseDTO batchSave(List<DeptWorkCalendar> calendarList) {
        LocalDateTime now = LocalDateTime.now();
        for (DeptWorkCalendar calendar : calendarList) {
            QueryWrapper<DeptWorkCalendar> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("dept_id", calendar.getDeptId())
                       .eq("work_date", calendar.getWorkDate());
            
            // 检查是否存在
            DeptWorkCalendar existing = getOne(queryWrapper);
            if (existing != null) {
                // 更新
                calendar.setId(existing.getId());
                calendar.setCreateTime(existing.getCreateTime());
                calendar.setUpdateTime(now);
                updateById(calendar);
            } else {
                // 新增
                calendar.setCreateTime(now);
                calendar.setUpdateTime(now);
                save(calendar);
            }
        }
        return Response.success();
    }

    /**
     * 查询部门指定月份的上班日历
     */
    public ResponseDTO getMonthCalendar(Integer deptId, String yearMonth) {
        // 解析年月
        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(4, 6));
        
        // 计算该月的第一天和最后一天
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // 查询该月所有日历记录
        QueryWrapper<DeptWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", deptId)
                   .ge("work_date", startDate)
                   .le("work_date", endDate)
                   .orderByAsc("work_date");
        
        List<DeptWorkCalendar> list = list(queryWrapper);
        
        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("deptId", deptId);
        result.put("yearMonth", yearMonth);
        result.put("calendarList", list);
        
        return Response.success(result);
    }

    /**
     * 查询部门某天是否上班
     */
    public ResponseDTO checkWorkday(Integer deptId, LocalDate workDate) {
        QueryWrapper<DeptWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", deptId)
                   .eq("work_date", workDate);
        
        DeptWorkCalendar calendar = getOne(queryWrapper);
        
        Map<String, Object> result = new HashMap<>();
        if (calendar != null) {
            result.put("isWorkday", calendar.getIsWorkday());
            result.put("remark", calendar.getRemark());
        } else {
            // 默认周一到周五上班,周末休息
            result.put("isWorkday", workDate.getDayOfWeek().getValue() <= 5 ? 1 : 0);
            result.put("remark", null);
        }
        
        return Response.success(result);
    }
}
