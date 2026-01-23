package com.abc.foodwastemanagement.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Rate-limit metadata used for HTTP response headers.
 */
@Data
@AllArgsConstructor
public class RateLimitInfo {

    private final int limit;              // max allowed tokens
    private final int remaining;          // remaining tokens
    private final long resetEpochSeconds; // next refill time
}
