package com.cow.controller;

import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Dept;
import com.cow.service.CheckinService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 考勤签到Controller
 */
@RestController
@RequestMapping("/checkin")
public class CheckinController {

    @Resource
    private CheckinService checkinService;

    /**
     * 员工签到
     */
    @PostMapping("/do")
    public ResponseDTO doCheckin(@RequestBody Map<String, Object> params) {
        Integer staffId = (Integer) params.get("staffId");
        Integer checkinType = (Integer) params.get("checkinType");
        String location = (String) params.get("location");
        return checkinService.doCheckin(staffId, checkinType, location);
    }

    /**
     * 查询员工今日签到记录
     */
    @GetMapping("/today/{staffId}")
    public ResponseDTO getTodayCheckins(@PathVariable Integer staffId) {
        return checkinService.getTodayCheckins(staffId);
    }

    /**
     * 查询签到状态
     */
    @GetMapping("/status/{staffId}")
    public ResponseDTO getCheckinStatus(@PathVariable Integer staffId) {
        return checkinService.getCheckinStatus(staffId);
    }

    /**
     * 获取员工所在部门的作息时间
     */
    @GetMapping("/dept-time/{staffId}")
    public ResponseDTO getDeptWorkTime(@PathVariable Integer staffId) {
        Dept dept = checkinService.getDeptByStaffId(staffId);
        if (dept == null) {
            return Response.error("未找到员工部门信息");
        }

        // 直接返回Dept对象，Jackson会根据@JsonFormat自动格式化为HH:mm
        return Response.success(dept);
    }
}
