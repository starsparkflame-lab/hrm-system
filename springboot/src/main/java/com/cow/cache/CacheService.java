package com.cow.cache;

/**
 * 缓存服务统一抽象，承载热点数据缓存与业务状态管理
 * <p>
 * 实现策略：优先使用 Redis；当 Redis 不可用（未安装、未启动、网络异常）时，
 * 通过熔断机制自动降级为进程内带 TTL 的本地缓存，保证业务不中断；
 * Redis 恢复后自动切回，无需重启应用。
 *
 * @Author : cow
 */
public interface CacheService {

    /**
     * 写入缓存
     *
     * @param key        缓存键
     * @param value      缓存值，使用 Redis 时需可 JDK 序列化
     * @param ttlSeconds 过期时间（秒），小于等于 0 表示不过期
     */
    void set(String key, Object value, long ttlSeconds);

    /**
     * 读取缓存
     *
     * @param key 缓存键
     * @return 缓存值，不存在或已过期返回 null
     */
    Object get(String key);

    /**
     * 删除缓存，Redis 与本地降级缓存中的同名键都会被清除
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断键是否存在且未过期
     *
     * @param key 缓存键
     */
    boolean hasKey(String key);

    /**
     * 当前生效的缓存模式
     *
     * @return redis 表示走 Redis，local 表示已降级为本地缓存
     */
    String mode();

    /**
     * 主动探测 Redis 此刻是否真的可达，会真实发起一次 PING 命令
     * <p>
     * 与 {@link #mode()} 的区别：mode() 只表示“当前是否打算用 Redis”，
     * 而本方法会实际连一次，用于运维排查“到底有没有 Redis”。
     *
     * @return true 表示 Redis 可用，false 表示未配置或连不上
     */
    boolean probeRedis();
}
