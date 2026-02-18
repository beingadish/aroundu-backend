package com.beingadish.AroundU.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised Resilience4j settings bound from
 * {@code resilience.circuitbreaker.*} and {@code resilience.retry.*} in YAML.
 * <p>
 * Each named instance (payment-gateway, email-service, image-upload) can be
 * tuned per environment profile.
 */
@Component
@ConfigurationProperties(prefix = "resilience")
@Getter
@Setter
public class ResilienceProperties {

    private CircuitBreakerSettings circuitbreaker = new CircuitBreakerSettings();
    private RetrySettings retry = new RetrySettings();

    // ── Circuit breaker ──────────────────────────────────────────────────
    @Getter
    @Setter
    public static class CircuitBreakerSettings {

        private InstanceCB paymentGateway = new InstanceCB();
        private InstanceCB emailService = new InstanceCB();
        private InstanceCB imageUpload = new InstanceCB();
    }

    @Getter
    @Setter
    public static class InstanceCB {

        /**
         * Failure-rate percentage to trip the breaker (e.g. 50 = 50 %).
         */
        private float failureRateThreshold = 50;
        /**
         * Seconds to stay in OPEN state before transitioning to HALF_OPEN.
         */
        private int waitDurationInOpenStateSeconds = 30;
        /**
         * Calls treated as slow (and counted as failures) if exceeding this.
         */
        private int slowCallDurationThresholdSeconds = 5;
        /**
         * Slow-call rate percentage to trip the breaker.
         */
        private float slowCallRateThreshold = 100;
        /**
         * Minimum number of calls required before the failure rate is
         * calculated.
         */
        private int minimumNumberOfCalls = 5;
        /**
         * Sliding-window size (number of calls).
         */
        private int slidingWindowSize = 10;
        /**
         * Calls allowed in HALF_OPEN state to test recovery.
         */
        private int permittedNumberOfCallsInHalfOpenState = 3;
    }

    // ── Retry ────────────────────────────────────────────────────────────
    @Getter
    @Setter
    public static class RetrySettings {

        private InstanceRetry paymentGateway = new InstanceRetry();
        private InstanceRetry emailService = new InstanceRetry();
        private InstanceRetry imageUpload = new InstanceRetry();
    }

    @Getter
    @Setter
    public static class InstanceRetry {

        /**
         * Maximum retry attempts (including the initial call).
         */
        private int maxAttempts = 3;
        /**
         * Base wait between retries in milliseconds.
         */
        private long waitDurationMs = 500;
        /**
         * Multiplier for exponential back-off.
         */
        private double multiplier = 2.0;
        /**
         * Random jitter factor (e.g. 0.1 = ±10 %).
         */
        private double randomizationFactor = 0.1;
    }
}
