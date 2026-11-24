package com.cow.controller;

import com.cow.cache.CacheService;
import com.cow.cache.LocalCacheStore;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存运行状态查询接口，用于排查「到底有没有用上 Redis」
 * <p>
 * 返回字段说明：
 * <ul>
 *     <li>redisReachable：此刻真实 PING 一次 Redis 的结果，true 表示 Redis 已安装且在运行；</li>
 *     <li>mode：当前业务实际使用的缓存模式，redis / local；</li>
 *     <li>localEntries：本地降级缓存中的条目数，降级期间该值会大于 0。</li>
 * </ul>
 * 该接口已在拦截器中放行，无需登录即可访问，仅返回非敏感的运行时状态。
 *
 * @Author : cow
 */
@RestController
@RequestMapping("/cache")
public class CacheStatusController {

    @Resource
    private CacheService cacheService;

    @Resource
    private LocalCacheStore localCacheStore;

    /**
     * 查询缓存运行状态
     */
    @GetMapping("/status")
    public ResponseDTO status() {
        Map<String, Object> data = new HashMap<>();
        data.put("redisReachable", cacheService.probeRedis());
        data.put("mode", cacheService.mode());
        data.put("localEntries", localCacheStore.size());
        return Response.success(data);
    }
}
