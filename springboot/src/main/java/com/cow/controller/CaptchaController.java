package com.cow.controller;

import com.cow.cache.CacheKeys;
import com.cow.cache.CacheService;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.wf.captcha.SpecCaptcha;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码接口
 * <p>
 * 验证码存放在缓存中（Redis 优先，Redis 不可用时自动降级为本地缓存），
 * 由 {@link CacheKeys#CAPTCHA_TTL} 控制真实的 5 分钟有效期，不再依赖 HttpSession 的容器超时。
 * 前端拿到 captchaKey 后需在登录/注册时一并回传，服务端据此取出验证码比对。
 *
 * @Author : cow
 * @Date : 2026/5/21
 */
@RestController
public class CaptchaController {

    @Resource
    private CacheService cacheService;

    /**
     * 生成验证码
     */
    @GetMapping("/captcha")
    public ResponseDTO captcha() {
        // 生成验证码
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
        String verCode = specCaptcha.text().toLowerCase();

        // 生成一次性标识，作为验证码在缓存中的键，前端登录/注册时回传
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        cacheService.set(CacheKeys.captcha(captchaKey), verCode, CacheKeys.CAPTCHA_TTL);

        // 返回验证码图片（Base64格式）
        String base64Image = specCaptcha.toBase64();

        // 确保返回的是完整的 data URI 格式
        if (base64Image != null && !base64Image.startsWith("data:")) {
            base64Image = "data:image/png;base64," + base64Image;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("captchaKey", captchaKey);
        data.put("image", base64Image);
        return Response.success(data);
    }
}
