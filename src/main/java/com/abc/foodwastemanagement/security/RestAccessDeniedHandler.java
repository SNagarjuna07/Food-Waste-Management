package com.abc.foodwastemanagement.security;

import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.exception.ErrorResponse;
import com.abc.foodwastemanagement.ratelimit.RateLimitInfo;
import com.abc.foodwastemanagement.ratelimit.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    private final RateLimiterService rateLimiterService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {

        if ("RATE_LIMIT_EXCEEDED".equals(ex.getMessage())) {

            String key = (String) request.getAttribute("RATE_LIMIT_KEY");

            RateLimitInfo info = rateLimiterService.getRateLimitInfo(key);

            long now = Instant.now().getEpochSecond();
            long retryAfter = Math.max(info.getResetEpochSeconds() - now, 1);

            // Standard rate-limit headers
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetEpochSeconds()));

            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    ErrorCode.REQUEST_LIMIT_EXCEEDED.name(),
                    "Too many requests. Please retry after " + retryAfter + " seconds."
            );

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }


        // NORMAL 403
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.UNAUTHORIZED_ACTION.name(),
                "You do not have permission to perform this action"
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}