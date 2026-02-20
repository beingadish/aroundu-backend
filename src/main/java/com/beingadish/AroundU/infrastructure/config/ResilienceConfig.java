package com.beingadish.AroundU.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Centralised Resilience4j configuration.
 * <p>
 * Creates named {@link CircuitBreaker} and {@link Retry} instances for every
 * external dependency, wires them into Micrometer for Prometheus scraping, and
 * publishes state-transition events to the application log.
 *
 * <pre>
 *  ┌─────────┐  failure threshold  ┌──────┐  wait duration  ┌───────────┐
 *  │ CLOSED  │ ──────────────────► │ OPEN │ ──────────────► │ HALF_OPEN │
 *  └─────────┘                     └──────┘                 └───────────┘
 *       ▲                                                        │
 *       └────────────── successful test calls ───────────────────┘
 * </pre>
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    // ── Circuit-Breaker names (used as bean qualifiers & metric tags) ─────
    public static final String CB_PAYMENT_GATEWAY = "payment-gateway";
    public static final String CB_EMAIL_SERVICE = "email-service";
    public static final String CB_IMAGE_UPLOAD = "image-upload";

    // ── Retry names ──────────────────────────────────────────────────────
    public static final String RETRY_PAYMENT_GATEWAY = "payment-gateway";
    public static final String RETRY_EMAIL_SERVICE = "email-service";
    public static final String RETRY_IMAGE_UPLOAD = "image-upload";

    // ═════════════════════════════════════════════════════════════════════
    //  CIRCUIT-BREAKER REGISTRY
    // ═════════════════════════════════════════════════════════════════════
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties props,
            MeterRegistry meterRegistry) {

        // --- Payment Gateway (strict) ---
        var paymentCBProps = props.getCircuitbreaker().getPaymentGateway();
        CircuitBreakerConfig paymentCBConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(paymentCBProps.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(paymentCBProps.getWaitDurationInOpenStateSeconds()))
                .slowCallDurationThreshold(Duration.ofSeconds(paymentCBProps.getSlowCallDurationThresholdSeconds()))
                .slowCallRateThreshold(paymentCBProps.getSlowCallRateThreshold())
                .minimumNumberOfCalls(paymentCBProps.getMinimumNumberOfCalls())
                .slidingWindowSize(paymentCBProps.getSlidingWindowSize())
                .permittedNumberOfCallsInHalfOpenState(paymentCBProps.getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        // --- Email Service (lenient) ---
        var emailCBProps = props.getCircuitbreaker().getEmailService();
        CircuitBreakerConfig emailCBConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(emailCBProps.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(emailCBProps.getWaitDurationInOpenStateSeconds()))
                .slowCallDurationThreshold(Duration.ofSeconds(emailCBProps.getSlowCallDurationThresholdSeconds()))
                .slowCallRateThreshold(emailCBProps.getSlowCallRateThreshold())
                .minimumNumberOfCalls(emailCBProps.getMinimumNumberOfCalls())
                .slidingWindowSize(emailCBProps.getSlidingWindowSize())
                .permittedNumberOfCallsInHalfOpenState(emailCBProps.getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        // --- Image Upload (medium) ---
        var imageCBProps = props.getCircuitbreaker().getImageUpload();
        CircuitBreakerConfig imageCBConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(imageCBProps.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(imageCBProps.getWaitDurationInOpenStateSeconds()))
                .slowCallDurationThreshold(Duration.ofSeconds(imageCBProps.getSlowCallDurationThresholdSeconds()))
                .slowCallRateThreshold(imageCBProps.getSlowCallRateThreshold())
                .minimumNumberOfCalls(imageCBProps.getMinimumNumberOfCalls())
                .slidingWindowSize(imageCBProps.getSlidingWindowSize())
                .permittedNumberOfCallsInHalfOpenState(imageCBProps.getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
                java.util.Map.of(
                        CB_PAYMENT_GATEWAY, paymentCBConfig,
                        CB_EMAIL_SERVICE, emailCBConfig,
                        CB_IMAGE_UPLOAD, imageCBConfig
                )
        );

        // Publish Micrometer metrics
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);

        // Event logging
        registerCircuitBreakerEventListeners(registry);

        return registry;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  RETRY REGISTRY
    // ═════════════════════════════════════════════════════════════════════
    @Bean
    public RetryRegistry retryRegistry(ResilienceProperties props,
            MeterRegistry meterRegistry) {

        // --- Payment Gateway: 3 retries, 500ms base, 2× multiplier ---
        var paymentRetryProps = props.getRetry().getPaymentGateway();
        RetryConfig paymentRetryConfig = RetryConfig.custom()
                .maxAttempts(paymentRetryProps.getMaxAttempts())
                .waitDuration(Duration.ofMillis(paymentRetryProps.getWaitDurationMs()))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(
                                paymentRetryProps.getWaitDurationMs(),
                                paymentRetryProps.getMultiplier(),
                                paymentRetryProps.getRandomizationFactor()))
                .retryExceptions(Exception.class)
                .build();

        // --- Email Service: 5 retries, 1000ms base ---
        var emailRetryProps = props.getRetry().getEmailService();
        RetryConfig emailRetryConfig = RetryConfig.custom()
                .maxAttempts(emailRetryProps.getMaxAttempts())
                .waitDuration(Duration.ofMillis(emailRetryProps.getWaitDurationMs()))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(
                                emailRetryProps.getWaitDurationMs(),
                                emailRetryProps.getMultiplier(),
                                emailRetryProps.getRandomizationFactor()))
                .retryExceptions(Exception.class)
                .build();

        // --- Image Upload: 2 retries, 200ms base ---
        var imageRetryProps = props.getRetry().getImageUpload();
        RetryConfig imageRetryConfig = RetryConfig.custom()
                .maxAttempts(imageRetryProps.getMaxAttempts())
                .waitDuration(Duration.ofMillis(imageRetryProps.getWaitDurationMs()))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(
                                imageRetryProps.getWaitDurationMs(),
                                imageRetryProps.getMultiplier(),
                                imageRetryProps.getRandomizationFactor()))
                .retryExceptions(Exception.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(
                java.util.Map.of(
                        RETRY_PAYMENT_GATEWAY, paymentRetryConfig,
                        RETRY_EMAIL_SERVICE, emailRetryConfig,
                        RETRY_IMAGE_UPLOAD, imageRetryConfig
                )
        );

        // Publish Micrometer metrics
        TaggedRetryMetrics.ofRetryRegistry(registry)
                .bindTo(meterRegistry);

        return registry;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NAMED BEAN ACCESSORS (for constructor injection)
    // ═════════════════════════════════════════════════════════════════════
    @Bean(name = "paymentGatewayCircuitBreaker")
    public CircuitBreaker paymentGatewayCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CB_PAYMENT_GATEWAY);
    }

    @Bean(name = "emailServiceCircuitBreaker")
    public CircuitBreaker emailServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CB_EMAIL_SERVICE);
    }

    @Bean(name = "imageUploadCircuitBreaker")
    public CircuitBreaker imageUploadCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CB_IMAGE_UPLOAD);
    }

    @Bean(name = "paymentGatewayRetry")
    public Retry paymentGatewayRetry(RetryRegistry registry) {
        return registry.retry(RETRY_PAYMENT_GATEWAY);
    }

    @Bean(name = "emailServiceRetry")
    public Retry emailServiceRetry(RetryRegistry registry) {
        return registry.retry(RETRY_EMAIL_SERVICE);
    }

    @Bean(name = "imageUploadRetry")
    public Retry imageUploadRetry(RetryRegistry registry) {
        return registry.retry(RETRY_IMAGE_UPLOAD);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  EVENT LISTENERS (logging + alerting hooks)
    // ═════════════════════════════════════════════════════════════════════
    private void registerCircuitBreakerEventListeners(CircuitBreakerRegistry registry) {
        registry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();

            cb.getEventPublisher()
                    .onStateTransition(event -> {
                        var transition = event.getStateTransition();
                        log.warn("CircuitBreaker [{}] state transition: {} → {}",
                                name, transition.getFromState(), transition.getToState());

                        if (transition.getToState() == CircuitBreaker.State.OPEN) {
                            log.error("ALERT: CircuitBreaker [{}] is now OPEN – "
                                    + "external dependency is failing. "
                                    + "Requests will be fast-failed.", name);
                        }
                        if (transition.getToState() == CircuitBreaker.State.CLOSED) {
                            log.info("CircuitBreaker [{}] recovered – back to CLOSED", name);
                        }
                    })
                    .onError(event
                            -> log.debug("CircuitBreaker [{}] recorded error: {}",
                            name, event.getThrowable().getMessage()))
                    .onSuccess(event
                            -> log.debug("CircuitBreaker [{}] recorded success ({}ms)",
                            name, event.getElapsedDuration().toMillis()));
        });
    }
}
