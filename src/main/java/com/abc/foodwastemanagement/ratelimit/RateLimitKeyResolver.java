package com.abc.foodwastemanagement.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the concrete identity value used for rate limiting.
 *
 * PURPOSE:
 * --------
 * Converts an abstract identity type (USER / IP)
 * into a concrete value for Redis key construction.
 *
 * EXAMPLES:
 * ---------
 * USER → username
 * IP   → client IP address
 */
@Component
public class RateLimitKeyResolver {

    /**
     * Resolves the identity value based on identity type.
     *
     * Priority:
     * ----------
     * 1. USER → authenticated username
     * 2. Fallback → client IP address
     */
    public String resolveIdentityValue(
            HttpServletRequest request,
            String identityType) {

        if ("USER".equals(identityType)) {

            Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();

            if (auth != null
                    && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {

                return auth.getName();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Builds the final Redis key for rate limiting.
     *
     * FORMAT:
     * -------
     * RATE_LIMIT:<PROFILE>:<IDENTITY>:<VALUE>
     */
    public String buildKey(
            String profile,
            String identity,
            HttpServletRequest request) {

        String identityValue =
                resolveIdentityValue(request, identity);

        return "RATE_LIMIT-" +
                profile + "-" +
                identity + ":" +
                identityValue;
    }
}
