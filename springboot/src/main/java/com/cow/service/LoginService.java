package com.cow.service;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.cache.CacheKeys;
import com.cow.cache.CacheService;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Staff;
import com.cow.enums.BusinessStatusEnum;
import com.cow.mapper.StaffMapper;
import com.cow.util.JWTUtil;
import com.cow.util.MD5Util;
import com.cow.vo.StaffDeptVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @Author : cow
 * @Date : 2022/1/30
 */

@Service
public class LoginService extends ServiceImpl<StaffMapper, Staff> {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private CacheService cacheService;

    public ResponseDTO login(Staff staff) {
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

        String password = MD5Util.MD55(staff.getPassword());
        StaffDeptVO staffDeptVO = this.staffMapper.findStaffInfo(staff.getCode(), password);
        if (staffDeptVO != null) {
            // 验证用户状态
            if (staffDeptVO.getStatus() == 1) {
                // 判断登录类型是否匹配
                boolean isAdminAccount = staffDeptVO.getId() == 1; // ID为1的是管理员账号
                boolean isAdminLogin = "admin".equals(staff.getLoginType());

                // 管理员账号只能用管理员方式登录
                if (isAdminAccount && !isAdminLogin) {
                    return Response.error("管理员账号请使用管理员登录方式!");
                }

                // 员工账号只能用员工方式登录
                if (!isAdminAccount && isAdminLogin) {
                    return Response.error("员工账号请使用员工登录方式!");
                }

                String token = JWTUtil.generateToken(staffDeptVO.getId(), password);

                // 写入 token 白名单，使登录态可被服务端主动撤销（登出、踢人下线）
                cacheService.set(CacheKeys.token(staffDeptVO.getId()), token, CacheKeys.TOKEN_TTL);
                // 密码可能已被重置，清掉旧的员工缓存，避免拦截器用过期数据校验
                cacheService.delete(CacheKeys.staff(staffDeptVO.getId()));

                // 返回员工信息、token和登录类型
                Map<String, Object> result = new HashMap<>();
                result.put("staff", staffDeptVO);
                result.put("loginType", staff.getLoginType());

                return Response.success(result, token);
            }
            return Response.error(BusinessStatusEnum.STAFF_STATUS_ERROR);
        }
        return Response.error("用户名或密码错误!");
    }

    /**
     * 退出登录：撤销 token 白名单，使当前 token 立即失效
     *
     * @param token 请求头中的 token
     */
    public ResponseDTO logout(String token) {
        Integer staffId = parseStaffId(token);
        if (staffId != null) {
            cacheService.delete(CacheKeys.token(staffId));
        }
        return Response.success("已退出登录");
    }

    /**
     * 踢人下线：管理员强制某个员工的登录态失效
     *
     * @param staffId 员工id
     */
    public ResponseDTO kickOut(Integer staffId) {
        if (staffId == null) {
            return Response.error("员工id不能为空!");
        }
        cacheService.delete(CacheKeys.token(staffId));
        return Response.success("已强制该员工下线");
    }

    /**
     * 从 token 中解析员工id，解析失败返回 null（登出接口不应因此报错）
     */
    private Integer parseStaffId(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            return Integer.valueOf(JWT.decode(token).getAudience().get(0));
        } catch (Exception e) {
            log.debug("登出时解析 token 失败，忽略：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送找回密码验证码
     * <p>
     * 验证码以手机号为键存入缓存，由 {@link CacheKeys#SMS_RESET_TTL} 实现真实的5分钟过期；
     * 同时用独立的限流键约束发送频率，避免被刷。
     */
    public ResponseDTO sendResetCode(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return Response.error("手机号不能为空!");
        }

        // 验证手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Response.error("手机号格式不正确!");
        }

        // 发送频率限制：60秒内只允许发送一次
        String limitKey = CacheKeys.smsResetLimit(phone);
        if (cacheService.hasKey(limitKey)) {
            return Response.error("验证码发送过于频繁，请60秒后再试!");
        }

        // 查询手机号是否存在（可能有多条记录）
        QueryWrapper<Staff> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        List<Staff> staffList = this.list(queryWrapper);

        if (staffList == null || staffList.isEmpty()) {
            return Response.error("该手机号未注册!");
        }

        // 如果有多条记录，取第一条（实际项目中应该确保手机号唯一）
        Staff staff = staffList.get(0);
        if (staffList.size() > 1) {
            log.warn("手机号 {} 对应多个账号，共 {} 个", phone, staffList.size());
        }

        // 生成6位数字验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 将验证码存入缓存，5分钟有效
        cacheService.set(CacheKeys.smsResetCode(phone), code, CacheKeys.SMS_RESET_TTL);
        cacheService.set(limitKey, "1", CacheKeys.SMS_SEND_INTERVAL_TTL);

        // TODO: 实际项目中应该发送短信验证码
        // 这里只是演示，实际应该调用短信服务
        log.info("重置密码验证码: {} (手机号: {})", code, phone);

        Map<String, Object> result = new HashMap<>();
        result.put("phone", phone);
        result.put("message", "验证码已发送(演示模式，请查看控制台输出: " + code + ")");

        return Response.success(result, "验证码已发送!");
    }

    /**
     * 重置密码
     */
    public ResponseDTO resetPassword(String code, String phone, String captchaCode, String newPassword) {
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(phone) || StringUtils.isEmpty(captchaCode) || StringUtils.isEmpty(newPassword)) {
            return Response.error("参数不能为空!");
        }

        // 验证新密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Response.error("密码长度应在6-20位之间!");
        }

        // 验证缓存中的短信验证码，超过5分钟自动失效
        String codeKey = CacheKeys.smsResetCode(phone);
        Object cachedCode = cacheService.get(codeKey);

        if (cachedCode == null) {
            return Response.error("验证码已过期，请重新获取!");
        }

        if (!cachedCode.toString().equals(captchaCode)) {
            return Response.error("验证码错误!");
        }

        // 根据员工编号查询用户（只更新当前输入的账号）
        QueryWrapper<Staff> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        Staff staff = this.getOne(queryWrapper);

        if (staff == null) {
            return Response.error("员工账号不存在!");
        }

        // 验证该账号的手机号是否匹配
        if (!phone.equals(staff.getPhone())) {
            return Response.error("该账号与输入的手机号不匹配!");
        }

        // 只更新当前账号的密码
        staff.setPassword(MD5Util.MD55(newPassword));
        this.updateById(staff);

        log.info("已重置员工账号 {} ({}) 的密码", code, staff.getName());

        // 验证码一次性使用，校验通过即作废
        cacheService.delete(codeKey);
        // 密码是 JWT 的签名密钥，改密后必须清理员工缓存并撤销登录态，
        // 否则拦截器会继续用缓存中的旧密码放行旧 token
        cacheService.delete(CacheKeys.staff(staff.getId()));
        cacheService.delete(CacheKeys.token(staff.getId()));

        return Response.success("密码重置成功!");
    }
}
