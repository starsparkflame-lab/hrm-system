package com.cow.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * {@link CacheService} 默认实现：Redis 优先 + 熔断降级到本地缓存
 * <p>
 * 降级语义：
 * <ul>
 *     <li>Redis 可用时以 Redis 为准，写入成功后会清掉本地同名键，避免降级期间的旧值干扰；</li>
 *     <li>Redis 操作抛异常即视为不可用，开启熔断，熔断期内所有请求直接走本地缓存，
 *         不再反复等待连接超时；</li>
 *     <li>熔断期结束后，下一次 Redis 操作即为探测，成功则自动切回 Redis；</li>
 *     <li>读取时若 Redis 未命中，会回查本地缓存，兼容「降级期间登录、Redis 随后恢复」的过渡场景，
 *         避免用户被无故踢下线。</li>
 * </ul>
 *
 * @Author : cow
 */
@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    /** 熔断时长：Redis 故障后 30 秒内不再尝试连接，直接走本地缓存 */
    private static final long CIRCUIT_OPEN_MILLIS = 30 * 1000L;

    /** Redis 未引入或未配置时为 null，此时全程使用本地缓存 */
    private final RedisTemplate<String, Object> redisTemplate;

    private final LocalCacheStore localCacheStore;

    /** 熔断恢复时间点，0 表示 Redis 正常可用 */
    private volatile long circuitOpenUntil = 0L;

    /** 是否处于降级状态，仅用于状态切换时输出日志 */
    private volatile boolean degraded = false;

    public CacheServiceImpl(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider,
                            LocalCacheStore localCacheStore) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.localCacheStore = localCacheStore;
        if (this.redisTemplate == null) {
            log.warn("未检测到 RedisTemplate，缓存将全程使用进程内本地缓存");
        }
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        if (redisUsable()) {
            try {
                if (ttlSeconds <= 0) {
                    this.redisTemplate.opsForValue().set(key, value);
                } else {
                    this.redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
                }
                // Redis 已成为该键的权威副本，清掉可能残留的本地降级值
                this.localCacheStore.delete(key);
                markRedisUp();
                return;
            } catch (SerializationException e) {
                // 值本身不可序列化，属于数据问题而非 Redis 故障，不触发降级，改写本地缓存
                log.error("缓存值序列化失败，改用本地缓存，key={}，valueType={}，原因：{}",
                        key, value == null ? "null" : value.getClass().getName(), e.getMessage());
            } catch (Exception e) {
                openCircuit(e);
            }
        }
        this.localCacheStore.set(key, value, ttlSeconds);
    }

    @Override
    public Object get(String key) {
        if (redisUsable()) {
            try {
                Object value = this.redisTemplate.opsForValue().get(key);
                markRedisUp();
                if (value != null) {
                    return value;
                }
            } catch (SerializationException e) {
                // 脏数据（如类结构变更后读到旧缓存），丢弃后按未命中处理，不能误判为 Redis 宕机
                log.warn("缓存值反序列化失败，已丢弃该键 {}：{}", key, e.getMessage());
                discardQuietly(key);
                return this.localCacheStore.get(key);
            } catch (Exception e) {
                openCircuit(e);
            }
        }
        // Redis 不可用，或 Redis 中不存在（可能写在降级期间的本地缓存里），回查本地
        return this.localCacheStore.get(key);
    }

    @Override
    public void delete(String key) {
        // 本地先删，保证降级模式下一定生效
        this.localCacheStore.delete(key);
        if (redisUsable()) {
            try {
                this.redisTemplate.delete(key);
                markRedisUp();
            } catch (Exception e) {
                openCircuit(e);
            }
        }
    }

    @Override
    public boolean hasKey(String key) {
        return get(key) != null;
    }

    @Override
    public String mode() {
        return redisUsable() ? "redis" : "local";
    }

    @Override
    public boolean probeRedis() {
        if (this.redisTemplate == null) {
            return false;
        }
        try {
            // 真实发起一次 PING，成功即代表 Redis 此刻可达
            this.redisTemplate.execute((RedisConnection connection) -> connection.ping());
            markRedisUp();
            return true;
        } catch (Exception e) {
            openCircuit(e);
            return false;
        }
    }

    /**
     * Redis 是否可尝试使用：模板存在且不在熔断期内
     */
    private boolean redisUsable() {
        return this.redisTemplate != null && System.currentTimeMillis() >= this.circuitOpenUntil;
    }

    /**
     * Redis 操作成功，若此前处于降级状态则记录恢复日志
     */
    private void markRedisUp() {
        if (this.degraded) {
            this.degraded = false;
            this.circuitOpenUntil = 0L;
            log.info("Redis 已恢复，缓存切回 Redis 模式");
        }
    }

    /**
     * 丢弃 Redis 中的脏数据，删除失败也不影响主流程
     */
    private void discardQuietly(String key) {
        try {
            this.redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("清理脏缓存键 {} 失败：{}", key, e.getMessage());
        }
    }

    /**
     * 开启熔断，并在恢复时间到达后由下一次操作自动探测
     */
    private void openCircuit(Exception e) {
        this.circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_OPEN_MILLIS;
        if (!this.degraded) {
            this.degraded = true;
            log.warn("Redis 不可用，已降级为本地缓存，{} 秒后自动重试。原因：{}",
                    CIRCUIT_OPEN_MILLIS / 1000, e.getMessage());
        } else if (log.isDebugEnabled()) {
            log.debug("Redis 仍不可用，继续保持本地缓存降级：{}", e.getMessage());
        }
    }
}
