package com.abc.foodwastemanagement.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result of rate-limit rule resolution.
 *
 * Contains:
 * ---------
 * - profile  → how strict
 * - identity → who to limit
 */
@Data
@AllArgsConstructor
public class RateLimitDecision {

    private final String profile;
    private final String identity;
}
