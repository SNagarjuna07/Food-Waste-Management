package com.abc.foodwastemanagement.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Post-authentication rate limiting filter.
 * Ensures rate limiting happens even for authenticated users.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitRuleResolver ruleResolver;

    @Autowired
    private RateLimitKeyResolver keyResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

                // Avoid consuming one more token
                if (request.getAttribute("RATE_LIMIT_KEY") != null) {
                    filterChain.doFilter(request, response);
                    return;
                }

        try {
            // Resolve rate-limit rule (profile + identity)
            RateLimitDecision decision = ruleResolver.resolve(request);

            // Skip unlimited endpoints
            if (!"UNLIMITED".equals(decision.getProfile())) {
                // Build Redis key
                String key = keyResolver.buildKey(
                    decision.getProfile(),
                    decision.getIdentity(),
                    request);

                // Store key for debugging / downstream use
                request.setAttribute("RATE_LIMIT_KEY", key);

                // Enforce rate limit
                rateLimiterService.checkRateLimit(key);
            }

        } catch (RedisConnectionFailureException ex) {
            // FAIL-OPEN: never block traffic if Redis is down
            log.warn("Redis unavailable (post-auth). Rate limiting skipped.");
        }

        // Continue filter chain (always - fail-open strategy)
        filterChain.doFilter(request, response);
    }
}