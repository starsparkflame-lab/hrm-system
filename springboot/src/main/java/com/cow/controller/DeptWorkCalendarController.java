package com.cow.controller;

import com.cow.dto.ResponseDTO;
import com.cow.entity.DeptWorkCalendar;
import com.cow.service.DeptWorkCalendarService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 部门上班日历控制器
 */
@RestController
@RequestMapping("/deptCalendar")
public class DeptWorkCalendarController {

    @Resource
    private DeptWorkCalendarService deptWorkCalendarService;

    @ApiOperation("批量保存部门上班日历")
    @PostMapping("/batchSave")
    public ResponseDTO batchSave(@RequestBody List<DeptWorkCalendar> calendarList) {
        return deptWorkCalendarService.batchSave(calendarList);
    }

    @ApiOperation("查询部门月份上班日历")
    @GetMapping("/month")
    public ResponseDTO getMonthCalendar(@RequestParam Integer deptId, 
                                        @RequestParam String yearMonth) {
        return deptWorkCalendarService.getMonthCalendar(deptId, yearMonth);
    }

    @ApiOperation("查询某天是否上班")
    @GetMapping("/checkWorkday")
    public ResponseDTO checkWorkday(@RequestParam Integer deptId, 
                                    @RequestParam String workDate) {
        LocalDate date = LocalDate.parse(workDate);
        return deptWorkCalendarService.checkWorkday(deptId, date);
    }
}
