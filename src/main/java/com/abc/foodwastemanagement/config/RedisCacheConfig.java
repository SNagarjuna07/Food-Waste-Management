package com.abc.foodwastemanagement.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * PRIMARY cache manager.
     * Uses Redis if available, otherwise falls back to NoOp cache.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        try {
            // Redis health check at startup
            connectionFactory.getConnection().close();
            return redisCacheManager(connectionFactory);

        } catch (Exception ex) {
            // Redis down -> disable caching safely
            return new NoOpCacheManager();
        }
    }

    /**
     * Actual Redis cache manager with TTL configuration
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        RedisSerializer.string()
                                )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        RedisSerializer.json()
                                )
                        )
                        .disableCachingNullValues()
                        .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("collectionCenters",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("activeCollectionCenters",
                defaultConfig.entryTtl(Duration.ofMinutes(2)));

        cacheConfigs.put("collectionCenterById",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        cacheConfigs.put("foodDonorsByUser",
                defaultConfig.entryTtl(Duration.ofMinutes(2)));

        cacheConfigs.put("foodDonorIdsByUser",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        cacheConfigs.put("userNotifications",
                defaultConfig.entryTtl(Duration.ofSeconds(30)));

        cacheConfigs.put("userUnreadNotificationCount",
                defaultConfig.entryTtl(Duration.ofSeconds(15)));

        cacheConfigs.put("userByUsername",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("userById",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
