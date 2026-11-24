package com.cow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>
 * 序列化方案说明：
 * <ul>
 *     <li>key 使用 String 序列化，便于用 redis-cli 直接查看和排查；</li>
 *     <li>value 使用 JDK 序列化。项目实体（Staff、Dept、City）均已实现 Serializable，
 *         而 GenderEnum 等自定义枚举在 JSON 往返时依赖 @JsonValue 的反向匹配，
 *         用 JDK 序列化可以完全规避枚举、Timestamp 等类型的反序列化歧义。</li>
 * </ul>
 * 因此缓存的值必须是可 JDK 序列化的对象；反序列化失败（如类结构变更后读到旧数据）
 * 由 {@link com.cow.cache.CacheServiceImpl} 统一按缓存未命中处理。
 *
 * @Author : cow
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer = new JdkSerializationRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
