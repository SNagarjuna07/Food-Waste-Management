package com.abc.foodwastemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.abc.foodwastemanagement.ratelimit.RateLimitBucket;

@Configuration
public class RedisConfig {

    /**
     * RedisTemplate for Rate Limiting
     *
     * Key   -> String (RATE_LIMIT:USER:123)
     * Value -> RateLimitBucket (stored as JSON)
     */
    @Bean
    public RedisTemplate<String, RateLimitBucket> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, RateLimitBucket> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(connectionFactory);

        // Key serializer (human-readable keys)
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // Value serializer (store Java object as JSON)
        redisTemplate.setValueSerializer(RedisSerializer.json());

        // Optional but recommended
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        
        return redisTemplate;
    }
}
