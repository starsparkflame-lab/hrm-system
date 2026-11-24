package com.cow.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.cache.CacheKeys;
import com.cow.cache.CacheService;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Staff;
import com.cow.mapper.StaffMapper;
import com.cow.util.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 注册服务
 *
 * @Author : cow
 * @Date : 2026/5/21
 */
@Service
public class RegisterService extends ServiceImpl<StaffMapper, Staff> {

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private CacheService cacheService;

    /**
     * 员工注册
     */
    public ResponseDTO register(Staff staff) {
        // 验证码校验：从缓存中按 captchaKey 取出，5分钟未使用自动过期
        String captcha = staff.getCaptcha();
        String captchaKey = staff.getCaptchaKey();
        if (StringUtils.isEmpty(captcha) || StringUtils.isEmpty(captchaKey)) {
            return Response.error("请输入验证码!");
        }

        String cacheKey = CacheKeys.captcha(captchaKey);
        Object cachedCaptcha = cacheService.get(cacheKey);
        if (cachedCaptcha == null) {
            return Response.error("验证码已过期，请刷新后重试!");
        }

        // 无论校验成功与否都作废，保证验证码一次性使用
        cacheService.delete(cacheKey);

        if (!captcha.toLowerCase().equals(cachedCaptcha.toString())) {
            return Response.error("验证码错误!");
        }
        
        // 参数校验
        if (StringUtils.isEmpty(staff.getCode())) {
            return Response.error("工号不能为空!");
        }
        if (StringUtils.isEmpty(staff.getName())) {
            return Response.error("姓名不能为空!");
        }
        if (StringUtils.isEmpty(staff.getPassword())) {
            return Response.error("密码不能为空!");
        }
        if (StringUtils.isEmpty(staff.getPhone())) {
            return Response.error("手机号不能为空!");
        }
        
        // 检查工号是否已存在
        Staff existStaff = this.lambdaQuery()
                .eq(Staff::getCode, staff.getCode())
                .one();
        if (existStaff != null) {
            return Response.error("该工号已存在!");
        }
        
        // 检查手机号是否已存在
        Staff existPhone = this.lambdaQuery()
                .eq(Staff::getPhone, staff.getPhone())
                .one();
        if (existPhone != null) {
            return Response.error("该手机号已被注册!");
        }
        
        // 密码加密
        staff.setPassword(MD5Util.MD55(staff.getPassword()));
        
        // 设置默认值
        staff.setStatus(1); // 默认在职
        staff.setGender(com.cow.enums.GenderEnum.MALE); // 默认男性
        staff.setDeptId(1); // 默认部门ID为1
        staff.setCreateTime(new Timestamp(System.currentTimeMillis()));
        staff.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        staff.setDeleteFlag(0);
        
        // 保存到数据库
        boolean result = this.save(staff);
        if (result) {
            return Response.success("注册成功!");
        } else {
            return Response.error("注册失败，请稍后重试!");
        }
    }
}
