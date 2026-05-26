package com.abc.foodwastemanagement.ratelimit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves which rate-limiting rule applies to an incoming request.
 *
 * THIS CLASS DOES:
 * ----------------
 * - Match request path + HTTP method against configured rules
 * - Select rate-limit profile (AUTH / READ / WRITE / UNLIMITED)
 * - Select identity type (USER / IP)
 *
 * THIS CLASS DOES NOT:
 * --------------------
 * - Extract user identity
 * - Access Redis
 * - Enforce rate limits
 * - Perform security checks
 *
 * DESIGN GOAL:
 * ------------
 * Pure rule resolution with no side effects.
 */
@Component
@RequiredArgsConstructor
public class RateLimitRuleResolver {

    private final RateLimitingProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    /*
     * Indexed rule storage
     */
    private Map<String, List<RateLimitingProperties.EndpointRule>> methodRules;
    private List<RateLimitingProperties.EndpointRule> pathOnlyRules;

    /*
     * Pre-processing happens ONCE at startup
     */
    @PostConstruct
    void initialize() {

        Map<String, List<RateLimitingProperties.EndpointRule>> methodMap = new HashMap<>();
        List<RateLimitingProperties.EndpointRule> pathRules = new ArrayList<>();

        for (RateLimitingProperties.EndpointRule rule : props.getEndpoints()) {

            if (rule.getMethod() != null) {
                String method = rule.getMethod().toUpperCase();
                methodMap
                        .computeIfAbsent(method, k -> new ArrayList<>())
                        .add(rule);
            } else {
                pathRules.add(rule);
            }
        }

        // Sort rules by path specificity (longest first)
        Comparator<RateLimitingProperties.EndpointRule> specificityComparator = 
        Comparator.<RateLimitingProperties.EndpointRule>comparingInt(
                r -> r.getPath().length()
        ).reversed();

        methodMap.values().forEach(list -> list.sort(specificityComparator));
        pathRules.sort(specificityComparator);

        // Freeze collections (thread-safe)
        methodMap.replaceAll((k, v) -> Collections.unmodifiableList(v));

        this.methodRules = Collections.unmodifiableMap(methodMap);
        this.pathOnlyRules = Collections.unmodifiableList(pathRules);
    }

    /*
     * Runtime resolution — fast & deterministic
     */
    public RateLimitDecision resolve(HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase();

        // Method + path match
        List<RateLimitingProperties.EndpointRule> rulesForMethod =
                methodRules.get(method);

        if (rulesForMethod != null) {
            for (RateLimitingProperties.EndpointRule rule : rulesForMethod) {
                if (matcher.match(rule.getPath(), path)) {
                    return new RateLimitDecision(
                            rule.getProfile(),
                            rule.getIdentity()
                    );
                }
            }
        }

        // Path-only match
        for (RateLimitingProperties.EndpointRule rule : pathOnlyRules) {
            if (matcher.match(rule.getPath(), path)) {
                return new RateLimitDecision(
                        rule.getProfile(),
                        rule.getIdentity()
                );
            }
        }

        // Global default
        return new RateLimitDecision(
                props.getDefaults().getProfile(),
                props.getDefaults().getIdentity()
        );
    }
}