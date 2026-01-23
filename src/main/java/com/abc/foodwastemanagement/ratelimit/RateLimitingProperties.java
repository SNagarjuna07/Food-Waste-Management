package com.abc.foodwastemanagement.ratelimit;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
// Represents all rate-limit rules defined in application.properties
public class RateLimitingProperties {

    /**
     * Rate-limit profiles (AUTH_STRICT, READ_LENIENT, WRITE_MODERATE)
     */
    private Map<String, Profile> profiles;

    /**
     * Endpoint-specific rules
     */
    private List<EndpointRule> endpoints;

    /**
     * Default fallback rule
     */
    private DefaultRule defaults;

    // ---------------- PROFILES ----------------

    @Getter
    @Setter
    public static class Profile {
        private int capacity;
        private int refillIntervalSeconds;
        private long ttlSeconds;
    }

    // ---------------- ENDPOINT RULES ----------------

    @Getter
    @Setter
    public static class EndpointRule {
        private String path;
        private String method;   
        private String profile;
        private String identity; 

        public String getIdentityOrDefault(String defaultIdentity) {
            return identity != null ? identity : defaultIdentity;
        }
    }

    // ---------------- DEFAULT RULE ----------------

    @Getter
    @Setter
    public static class DefaultRule {
        private String profile;
        private String identity;
    }
}
