package com.cow.cache;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 不可用时的降级存储：进程内 ConcurrentHashMap + 过期时间戳
 * <p>
 * 过期采用「读取时惰性淘汰 + 定时批量清理」双重策略：
 * 惰性淘汰保证读到的值一定未过期，定时清理避免已过期但不再被访问的键长期占用内存。
 *
 * @Author : cow
 */
@Component
public class LocalCacheStore {

    /** 缓存条目，携带绝对过期时间点 */
    private static final class Entry {

        private final Object value;

        private final long expireAt;

        Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean expired() {
            return System.currentTimeMillis() > this.expireAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void set(String key, Object value, long ttlSeconds) {
        // ttl <= 0 视为永不过期，用 Long.MAX_VALUE 表示，避免加法溢出
        long expireAt = ttlSeconds <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttlSeconds * 1000L;
        this.store.put(key, new Entry(value, expireAt));
    }

    public Object get(String key) {
        Entry entry = this.store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            // 惰性淘汰
            this.store.remove(key);
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        this.store.remove(key);
    }

    /**
     * 每分钟清理一次过期条目
     */
    @Scheduled(fixedDelay = 60 * 1000L)
    public void cleanExpired() {
        Iterator<Map.Entry<String, Entry>> iterator = this.store.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expired()) {
                iterator.remove();
            }
        }
    }

    /**
     * 当前本地缓存条目数，便于观察降级期间的内存占用
     */
    public int size() {
        return this.store.size();
    }
}
