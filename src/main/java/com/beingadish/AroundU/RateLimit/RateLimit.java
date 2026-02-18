package com.beingadish.AroundU.RateLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method for per-user rate limiting.
 * <p>
 * Each authenticated user (or IP for anonymous requests) gets an independent
 * token bucket. When tokens are exhausted the request is rejected with HTTP 429
 * (Too Many Requests).
 *
 * <pre>
 * &#64;RateLimit(capacity = 5, refillTokens = 5, refillMinutes = 60)
 * public ResponseEntity&lt;?&gt; createJob(...) { ... }
 * </pre>
 *
 * @see RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of tokens (requests) the bucket can hold.
     */
    int capacity() default 10;

    /**
     * Number of tokens added each refill period.
     */
    int refillTokens() default 10;

    /**
     * Refill period in minutes.
     */
    int refillMinutes() default 1;
}
