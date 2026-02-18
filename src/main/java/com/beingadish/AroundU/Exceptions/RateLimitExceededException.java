package com.beingadish.AroundU.Exceptions;

import lombok.Getter;

/**
 * Thrown when a user exceeds the rate limit for a given endpoint. Carries
 * metadata so the exception handler can populate the correct response headers
 * (Retry-After, X-RateLimit-*).
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int limit;
    private final long retryAfterSeconds;

    public RateLimitExceededException(int limit, long retryAfterSeconds) {
        super("Too many requests. Try again in %d seconds.".formatted(retryAfterSeconds));
        this.limit = limit;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
