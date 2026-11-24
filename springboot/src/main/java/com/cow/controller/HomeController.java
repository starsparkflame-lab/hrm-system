package com.cow.controller;

import com.cow.dto.ResponseDTO;
import com.cow.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 
 * @Date 2022/2/27
 * @Version 1.0
 */

@RestController
@RequestMapping("/home")
public class HomeController {

    @Resource
    private HomeService homeService;

    @GetMapping("/staff")
    public ResponseDTO getStaffData() {
        return this.homeService.getStaffData();
    }

    /**
     * 获取统计数据
     * @param deptId 部门ID(可选,不传则统计全部)
     * @param loginType 登录类型(admin=管理员,staff=员工)
     */
    @GetMapping("/count")
    public ResponseDTO getCountData(Integer deptId, String loginType) {
        return this.homeService.getCountData(deptId, loginType);
    }

    @GetMapping("/city")
    public ResponseDTO getCityData() {
        return this.homeService.getCityData();
    }

    @GetMapping("/attendance")
    public ResponseDTO getAttendanceData(@RequestParam Integer id, String month) {
        return this.homeService.getAttendanceData(id, month);
    }

    @GetMapping("/department")
    public ResponseDTO getDepartmentData() {
        return this.homeService.getDepartmentData();
    }

}
