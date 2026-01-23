package com.abc.foodwastemanagement.config;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CacheErrorHandlingConfig {

    @Bean
    public CacheErrorHandler cacheErrorHandler() {

        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {

                log.warn("Cache GET failed. Cache={}, key={}, falling back to DB", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {

                log.warn("Cache PUT failed. Cache={}, key={}, ignoring", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {

                log.warn("Cache EVICT failed. Cache={}, key={}, ignoring", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {

                log.warn("Cache CLEAR failed. Cache={}, ignoring", cache.getName(), exception);
            }
        };
    }
}
