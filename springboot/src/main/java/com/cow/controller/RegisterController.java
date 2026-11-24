package com.cow.controller;

import com.cow.annotation.OperationLog;
import com.cow.entity.Staff;
import com.cow.dto.ResponseDTO;
import com.cow.service.RegisterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 注册接口
 *
 * @Author : cow
 * @Date : 2026/5/21
 */
@RestController
public class RegisterController {

    @Resource
    private RegisterService registerService;

    /**
     * 员工注册
     */
    @PostMapping("/register")
    @OperationLog(title = "员工注册", businessType = 1)
    public ResponseDTO register(@RequestBody Staff staff) {
        return this.registerService.register(staff);
    }
}
