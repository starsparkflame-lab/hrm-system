package com.cow.controller;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Staff;
import com.cow.service.CalendarService;
import com.cow.service.StaffService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 上班日历控制器
 */
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Resource
    private CalendarService calendarService;

    @Resource
    private StaffService staffService;

    @ApiOperation("获取部门考勤日历")
    @GetMapping("/dept")
    public ResponseDTO getDeptCalendar(@RequestParam(required = false) Integer deptId, 
                                        @RequestParam(required = false) String month,
                                        HttpServletRequest request) {
        // 获取当前登录用户信息
        String token = request.getHeader("token");
        Integer currentStaffDeptId = null;
        String currentStaffRole = null;
        
        if (StrUtil.isNotBlank(token)) {
            try {
                Integer staffId = Integer.valueOf(JWT.decode(token).getAudience().get(0));
                Staff staff = staffService.getById(staffId);
                if (staff != null) {
                    currentStaffDeptId = staff.getDeptId();
                    // 使用loginType判断权限：admin为管理员，staff为普通员工
                    currentStaffRole = staff.getLoginType();
                    System.out.println("[DEBUG Controller] staffId=" + staffId + ", loginType=" + currentStaffRole + ", deptId=" + currentStaffDeptId);
                }
            } catch (Exception e) {
                System.out.println("[DEBUG Controller] token解析失败: " + e.getMessage());
                // token解析失败,使用默认权限
            }
        } else {
            System.out.println("[DEBUG Controller] token为空");
        }
        System.out.println("[DEBUG Controller] 传递给Service的参数 - deptId=" + deptId + ", currentStaffDeptId=" + currentStaffDeptId + ", currentStaffRole=" + currentStaffRole);
        
        return calendarService.getDeptCalendar(deptId, month, currentStaffDeptId, currentStaffRole);
    }
}
