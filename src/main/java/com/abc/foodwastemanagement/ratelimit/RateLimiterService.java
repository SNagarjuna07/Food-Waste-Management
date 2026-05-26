package com.abc.foodwastemanagement.ratelimit;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * RateLimiterService
 * Engine of rate limiting
 * Token Bucket implementation.
 *
 * Profile-based capacity
 * Profile-based refill rate
 * Profile-based TTL
 * TTL preserved on updates
 * Retry-After support
 * X-RateLimit headers support
 *
 * Functionality:
 * Loads bucket from Redis
 * Refills tokens
 * Checks limits
 * Consumes tokens
 * Saves bucket back
 * Throws exception when limit is exceeded
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, RateLimitBucket> redisTemplate;
    private final RateLimitingProperties properties;

    /**
     * Enforces rate limiting for the given Redis key.
     *
     * Key format:
     * RATE_LIMIT:<PROFILE>:<IDENTITY>:<VALUE>
     */
    public void checkRateLimit(String key) {
        try {
            long now = Instant.now().getEpochSecond();
            RateLimitBucket bucket = redisTemplate.opsForValue().get(key);

            // FIRST REQUEST
            if (bucket == null) {
                int capacity = resolveCapacity(key);
                RateLimitBucket newBucket = new RateLimitBucket(capacity - 1, now);
                long ttlSeconds = resolveTtlSeconds(key);
                redisTemplate.opsForValue().set(key, newBucket, ttlSeconds, TimeUnit.SECONDS);
                return;
            }

            // REFILL
            long elapsedSeconds = now - bucket.getLastRefillTimestamp();
            int refillInterval = resolveRefillIntervalSeconds(key);
            int tokensToAdd = (int) (elapsedSeconds / refillInterval);

            if (tokensToAdd > 0) {
                int capacity = resolveCapacity(key);
                int updatedTokens = Math.min(capacity, bucket.getTokens() + tokensToAdd);
                bucket.setTokens(updatedTokens);
                bucket.setLastRefillTimestamp(
                    bucket.getLastRefillTimestamp() + (long) tokensToAdd * refillInterval);
            }

            // CHECK LIMIT (ONLY allowed exception)
            if (bucket.getTokens() < 1) {
                throw new AccessDeniedException("RATE_LIMIT_EXCEEDED");
            }

            // CONSUME
            bucket.setTokens(bucket.getTokens() - 1);

            // SAVE (TTL SAFE)
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                redisTemplate.opsForValue().set(key, bucket, ttl, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, bucket);
            }

        } catch (AccessDeniedException e) {
            // RATE LIMIT HIT → MUST propagate
            throw e;
        } catch (Exception e) {
            // FAIL-OPEN FOR ALL REDIS / INFRA FAILURES
            log.warn("Rate limiting skipped due to Redis failure. Key={}, Reason={}",
                    key, e.getClass().getSimpleName());
            return;
        }
    }

    /**
     * Calculates Retry-After value (seconds).
     */
    public long getRetryAfterSeconds(String key) {
        RateLimitBucket bucket = redisTemplate.opsForValue().get(key);
        if (bucket == null) {
            return 0;
        }

        long now = Instant.now().getEpochSecond();
        int refillInterval = resolveRefillIntervalSeconds(key);
        long nextRefillTime = bucket.getLastRefillTimestamp() + refillInterval;
        return Math.max(nextRefillTime - now, 1);
    }

    /**
     * Provides data for X-RateLimit-* headers.
     */
    public RateLimitInfo getRateLimitInfo(String key) {
        RateLimitBucket bucket = redisTemplate.opsForValue().get(key);
        long now = Instant.now().getEpochSecond();
        int capacity = resolveCapacity(key);
        int refillInterval = resolveRefillIntervalSeconds(key);

        if (bucket == null) {
            return new RateLimitInfo(capacity, capacity, now);
        }

        long nextReset = bucket.getLastRefillTimestamp() + refillInterval;
        return new RateLimitInfo(capacity, bucket.getTokens(), nextReset);
    }

    /* =================================================
    RESOLUTION METHODS (CONFIG-DRIVEN)
    ================================================= */
    private String extractProfile(String key) {
        // RATE_LIMIT:<PROFILE>:<IDENTITY>:<VALUE>
        return key.split("-")[1];
    }

    private int resolveCapacity(String key) {
        String profile = extractProfile(key);
        return properties.getProfiles().get(profile).getCapacity();
    }

    private int resolveRefillIntervalSeconds(String key) {
        String profile = extractProfile(key);
        return properties.getProfiles().get(profile).getRefillIntervalSeconds();
    }

    private long resolveTtlSeconds(String key) {
        String profile = extractProfile(key);
        return properties.getProfiles().get(profile).getTtlSeconds();
    }
}
