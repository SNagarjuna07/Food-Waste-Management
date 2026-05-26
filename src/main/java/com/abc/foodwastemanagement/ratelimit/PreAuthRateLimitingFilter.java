package com.abc.foodwastemanagement.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Pre-authentication rate limiting filter.
 *
 * Responsibilities:
 * - Apply rate limiting BEFORE Spring Security
 * - Return 429 when limit is exceeded
 * - Add X-RateLimit headers
 * - Fail-open when Redis is unavailable
 *
 * MUST NOT:
 * - Throw exceptions to Spring Security
 * - Call chain.doFilter() after writing 429
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PreAuthRateLimitingFilter implements Filter {

    private final RateLimiterService rateLimiterService;

    private final RateLimitRuleResolver ruleResolver;

    private final RateLimitKeyResolver keyResolver;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String rateLimitKey = null;

        try {
            // Resolve rate-limit rule (profile + identity)
            RateLimitDecision decision = ruleResolver.resolve(httpRequest);

            // Skip unlimited endpoints
            if (!"UNLIMITED".equals(decision.getProfile())) {
                //  Build Redis key
                rateLimitKey = keyResolver.buildKey(
                    decision.getProfile(),
                    decision.getIdentity(),
                    httpRequest);

                // Store key for debugging / downstream use
                request.setAttribute("RATE_LIMIT_KEY", rateLimitKey);

                // Enforce rate limit
                rateLimiterService.checkRateLimit(rateLimitKey);

                // Add rate-limit headers for allowed request
                RateLimitInfo info = rateLimiterService.getRateLimitInfo(rateLimitKey);
                addRateLimitHeaders(httpResponse, info);
            }

        } catch (AccessDeniedException ex) {
            // RATE LIMIT EXCEEDED — HANDLE HERE (NOT IN SPRING SECURITY)
            log.warn("Rate limit exceeded for key {}", rateLimitKey);

            if (rateLimitKey != null) {
                RateLimitInfo info = rateLimiterService.getRateLimitInfo(rateLimitKey);
                addRateLimitHeaders(httpResponse, info);
            }

            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("""
                {
                    "status": 429,
                    "code": "RATE_LIMIT_EXCEEDED",
                    "message": "Too many requests. Please try again later."
                }
                """);
                
            return; // DO NOT CONTINUE — PREVENT SPRING SECURITY

        } catch (RedisConnectionFailureException ex) {
            // FAIL-OPEN: never block traffic if Redis is down
            log.warn("Redis unavailable (pre-auth). Rate limiting skipped.");
        }

        // 6️⃣ Only allowed requests reach Spring Security
        chain.doFilter(request, response);
    }

    /**
     * Adds standard rate-limit response headers.
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitInfo info) {

        response.setHeader(
            "X-RateLimit-Limit",
            String.valueOf(info.getLimit())
        );
        response.setHeader(
            "X-RateLimit-Remaining",
            String.valueOf(info.getRemaining())
        );
        response.setHeader(
            "X-RateLimit-Reset",
            String.valueOf(info.getResetEpochSeconds())
        );

        long retryAfter = info.getResetEpochSeconds() - Instant.now().getEpochSecond();
        if (retryAfter > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
        }
    }
}
