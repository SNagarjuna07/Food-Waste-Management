package com.abc.foodwastemanagement.ratelimit;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Redis-stored token bucket.
 *
 * Represents the current state of rate limiting
 * for a specific key.
 */
@Data
@AllArgsConstructor
public class RateLimitBucket implements Serializable {

    private int tokens;               // remaining tokens
    private long lastRefillTimestamp; // last refill epoch second
}
