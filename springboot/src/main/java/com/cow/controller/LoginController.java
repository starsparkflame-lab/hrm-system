package com.cow.controller;

import com.cow.annotation.OperationLog;
import com.cow.entity.Staff;
import com.cow.dto.ResponseDTO;
import com.cow.service.LoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 登录注册接口
 *
 * @Author : cow
 * @Date : 2022/1/30
 */
@RestController
public class LoginController {

    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    @OperationLog(title = "登录", businessType = 4)
    public ResponseDTO login(@RequestBody Staff staff) {
        return this.loginService.login(staff);
    }

    /**
     * 退出登录，撤销服务端的 token 白名单，使当前 token 立即失效
     */
    @PostMapping("/logout")
    @OperationLog(title = "退出登录", businessType = 5)
    public ResponseDTO logout(@RequestHeader(value = "token", required = false) String token) {
        return this.loginService.logout(token);
    }

    @PostMapping("/forgot-password/send-code")
    @OperationLog(title = "发送重置密码验证码")
    public ResponseDTO sendResetCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        return this.loginService.sendResetCode(phone);
    }

    @PostMapping("/forgot-password/reset")
    @OperationLog(title = "重置密码", businessType = 2)
    public ResponseDTO resetPassword(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        String phone = params.get("phone");
        String captchaCode = params.get("captchaCode");
        String newPassword = params.get("newPassword");
        return this.loginService.resetPassword(code, phone, captchaCode, newPassword);
    }
}
