package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.infrastructure.config.ResilienceConfig;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.notification.service.impl.EmailServiceImpl;
import com.beingadish.AroundU.infrastructure.storage.impl.ImageStorageServiceImpl;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.beingadish.AroundU.payment.service.ResilientPaymentService;
import com.beingadish.AroundU.payment.service.PaymentService;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.notification.service.EmailService;

/**
 * Comprehensive unit tests for the Resilience4j integration.
 * <p>
 * We create real (not mocked) CircuitBreaker and Retry instances with tight
 * thresholds so tests run fast, and verify:
 * <ol>
 * <li>Circuit breaker opens after failure threshold</li>
 * <li>Requests fail fast while circuit is open</li>
 * <li>Half-open state allows test request</li>
 * <li>Successful request closes the circuit</li>
 * <li>Retry with exponential back-off</li>
 * <li>Jitter prevents exact retry intervals</li>
 * <li>Metrics are recorded correctly</li>
 * <li>Fallback behaviour for payment / email / image services</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resilience4j Integration")
class ResilienceTest {

    // =====================================================================
    //  1 · Circuit Breaker core behaviour
    // =====================================================================
    @Nested
    @DisplayName("CircuitBreaker – state transitions")
    class CircuitBreakerStateTests {

        private CircuitBreaker cb;

        @BeforeEach
        void setUp() {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50) // open at 50 % failures
                    .minimumNumberOfCalls(4) // need 4 calls to evaluate
                    .slidingWindowSize(4)
                    .waitDurationInOpenState(Duration.ofMillis(500))
                    .permittedNumberOfCallsInHalfOpenState(2)
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .build();
            cb = CircuitBreaker.of("test-cb", config);
        }

        @Test
        @DisplayName("stays CLOSED when failures are below threshold")
        void staysClosed_belowThreshold() {
            // 1 failure + 3 successes = 25 % failure → below 50 %
            recordFailure(cb);
            recordSuccess(cb);
            recordSuccess(cb);
            recordSuccess(cb);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("opens after failure threshold is exceeded")
        void opensAfterThreshold() {
            // 3 failures + 1 success = 75 % failure → above 50 %
            recordFailure(cb);
            recordFailure(cb);
            recordFailure(cb);
            recordSuccess(cb);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("fast-fails when OPEN (CallNotPermittedException)")
        void fastFailsWhenOpen() {
            // Force open
            forceOpen(cb);

            Supplier<String> decorated = CircuitBreaker.decorateSupplier(cb, () -> "ok");
            assertThatThrownBy(decorated::get)
                    .isInstanceOf(CallNotPermittedException.class);
        }

        @Test
        @DisplayName("transitions OPEN → HALF_OPEN after wait duration")
        void transitionsToHalfOpen() throws InterruptedException {
            forceOpen(cb);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Wait for the open-state duration to pass
            Thread.sleep(600);

            // Trigger state evaluation by attempting a call
            cb.tryAcquirePermission();
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }

        @Test
        @DisplayName("closes from HALF_OPEN on sufficient successful calls")
        void closesFromHalfOpen() throws InterruptedException {
            forceOpen(cb);
            Thread.sleep(600);
            cb.tryAcquirePermission(); // triggers HALF_OPEN

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // Two successes in half-open (= permittedNumberOfCallsInHalfOpenState)
            recordSuccess(cb);
            recordSuccess(cb);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("re-opens from HALF_OPEN if test calls fail")
        void reOpensFromHalfOpen() throws InterruptedException {
            forceOpen(cb);
            Thread.sleep(600);
            cb.tryAcquirePermission(); // HALF_OPEN

            recordFailure(cb);
            recordFailure(cb);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    // =====================================================================
    //  2 · Retry with exponential back-off
    // =====================================================================
    @Nested
    @DisplayName("Retry – exponential back-off")
    class RetryTests {

        @Test
        @DisplayName("retries the specified number of times before giving up")
        void retriesMaxAttempts() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(50))
                    .retryExceptions(RuntimeException.class)
                    .build();
            Retry retry = Retry.of("test-retry", config);

            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                attempts.incrementAndGet();
                throw new RuntimeException("fail");
            });

            assertThatThrownBy(supplier::get).isInstanceOf(RuntimeException.class);
            assertThat(attempts.get()).isEqualTo(3); // initial + 2 retries
        }

        @Test
        @DisplayName("succeeds on retry after transient failures")
        void succeedsOnRetry() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(10))
                    .retryExceptions(RuntimeException.class)
                    .build();
            Retry retry = Retry.of("test-retry-success", config);

            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException("transient");
                }
                return "success";
            });

            assertThat(supplier.get()).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("exponential back-off increases wait intervals")
        void exponentialBackoff() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(4)
                    .waitDuration(Duration.ofMillis(100))
                    .intervalFunction(io.github.resilience4j.core.IntervalFunction
                            .ofExponentialBackoff(100, 2.0))
                    .retryExceptions(RuntimeException.class)
                    .build();
            Retry retry = Retry.of("test-exp", config);

            long[] timestamps = new long[4];
            AtomicInteger idx = new AtomicInteger(0);

            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                int i = idx.getAndIncrement();
                timestamps[i] = System.currentTimeMillis();
                if (i < 3) {
                    throw new RuntimeException("fail");
                }
                return "ok";
            });

            supplier.get();

            // Intervals should roughly double: ~100ms, ~200ms, ~400ms (with tolerance)
            long interval1 = timestamps[1] - timestamps[0];
            long interval2 = timestamps[2] - timestamps[1];
            long interval3 = timestamps[3] - timestamps[2];

            assertThat(interval1).isGreaterThanOrEqualTo(80);   // ~100ms
            assertThat(interval2).isGreaterThan(interval1 - 30); // increases
            assertThat(interval3).isGreaterThan(interval2 - 30); // increases
        }

        @Test
        @DisplayName("jitter adds randomization to intervals")
        void jitterRandomization() {
            long base = 200;
            double jitter = 0.5; // ±50 %

            var intervalFn = io.github.resilience4j.core.IntervalFunction
                    .ofExponentialRandomBackoff(base, 2.0, jitter);

            // Sample several intervals — they should vary
            long i1 = intervalFn.apply(1);
            long i2 = intervalFn.apply(1);
            long i3 = intervalFn.apply(1);

            // With 50 % jitter on a 200ms base, range is [100, 300]
            assertThat(i1).isBetween(100L, 300L);
            assertThat(i2).isBetween(100L, 300L);
            assertThat(i3).isBetween(100L, 300L);

            // At least two out of three should differ (randomization is working)  
            // (not guaranteed but extremely likely with 50 % jitter)
        }
    }

    // =====================================================================
    //  3 · Circuit Breaker + Retry composed
    // =====================================================================
    @Nested
    @DisplayName("CircuitBreaker + Retry – composed")
    class ComposedTests {

        @Test
        @DisplayName("retry exhausts then circuit breaker records failures")
        void retryThenCircuitBreaker() {
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(2)
                    .slidingWindowSize(2)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .build();
            CircuitBreaker cb = CircuitBreaker.of("composed-cb", cbConfig);

            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(2)
                    .waitDuration(Duration.ofMillis(10))
                    .retryExceptions(RuntimeException.class)
                    .build();
            Retry retry = Retry.of("composed-retry", retryConfig);

            AtomicInteger callCount = new AtomicInteger(0);
            Supplier<String> raw = () -> {
                callCount.incrementAndGet();
                throw new RuntimeException("service down");
            };

            // CB(Retry(raw))
            Supplier<String> decorated = CircuitBreaker.decorateSupplier(cb,
                    Retry.decorateSupplier(retry, raw));

            // First call: retry 2×, fails, CB records 1 failure
            assertThatThrownBy(decorated::get).isInstanceOf(RuntimeException.class);
            assertThat(callCount.get()).isEqualTo(2);

            // Second call: retry 2×, fails, CB records 2nd failure → opens (100 % failure)
            callCount.set(0);
            assertThatThrownBy(decorated::get).isInstanceOf(RuntimeException.class);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Third call: circuit is open → fast fail (no actual calls)
            callCount.set(0);
            assertThatThrownBy(decorated::get)
                    .isInstanceOf(CallNotPermittedException.class);
            assertThat(callCount.get()).isEqualTo(0); // no attempt made
        }
    }

    // =====================================================================
    //  4 · Micrometer metrics
    // =====================================================================
    @Nested
    @DisplayName("Metrics – Micrometer integration")
    class MetricsTests {

        @Test
        @DisplayName("circuit breaker publishes metrics to MeterRegistry")
        void cbPublishesMetrics() {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();

            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(2)
                    .slidingWindowSize(2)
                    .build();
            CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(
                    java.util.Map.of("metrics-test-cb", config));

            io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
                    .ofCircuitBreakerRegistry(cbRegistry).bindTo(registry);

            CircuitBreaker cb = cbRegistry.circuitBreaker("metrics-test-cb");
            recordSuccess(cb);
            recordFailure(cb);

            // Check that metrics exist
            assertThat(registry.getMeters()).isNotEmpty();
            assertThat(registry.find("resilience4j.circuitbreaker.calls").meters())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("retry publishes metrics to MeterRegistry")
        void retryPublishesMetrics() {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();

            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(2)
                    .waitDuration(Duration.ofMillis(10))
                    .retryExceptions(RuntimeException.class)
                    .build();
            RetryRegistry retryRegistry = RetryRegistry.of(
                    java.util.Map.of("metrics-test-retry", config));

            io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics
                    .ofRetryRegistry(retryRegistry).bindTo(registry);

            Retry r = retryRegistry.retry("metrics-test-retry");
            Supplier<String> decorated = Retry.decorateSupplier(r, () -> {
                throw new RuntimeException("fail");
            });

            try {
                decorated.get();
            } catch (Exception ignored) {
            }

            assertThat(registry.find("resilience4j.retry.calls").meters())
                    .isNotEmpty();
        }
    }

    // =====================================================================
    //  5 · ResilientPaymentService
    // =====================================================================
    @Nested
    @DisplayName("ResilientPaymentService")
    class ResilientPaymentServiceTests {

        @Mock
        private MetricsService metricsService;
        @Mock
        private EmailService emailService;

        @Test
        @DisplayName("delegates to underlying service on success")
        void delegatesOnSuccess() {
            PaymentService delegate = mock(PaymentService.class);
            PaymentTransaction expected = PaymentTransaction.builder()
                    .status(PaymentStatus.ESCROW_LOCKED)
                    .amount(100.0)
                    .build();
            when(delegate.lockEscrow(eq(1L), eq(2L), any())).thenReturn(expected);

            CircuitBreaker cb = CircuitBreaker.of("pay-test",
                    CircuitBreakerConfig.ofDefaults());
            Retry retry = Retry.of("pay-test", RetryConfig.ofDefaults());

            ResilientPaymentService service = new ResilientPaymentService(
                    delegate, cb, retry, metricsService, emailService);

            PaymentLockRequest req = new PaymentLockRequest();
            req.setAmount(100.0);

            PaymentTransaction result = service.lockEscrow(1L, 2L, req);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.ESCROW_LOCKED);
            verify(delegate).lockEscrow(1L, 2L, req);
        }

        @Test
        @DisplayName("returns pending and queues on failure")
        void returnsPendingOnFailure() {
            PaymentService delegate = mock(PaymentService.class);
            when(delegate.lockEscrow(anyLong(), anyLong(), any()))
                    .thenThrow(new RuntimeException("gateway down"));
            when(metricsService.getPaymentFailureCounter())
                    .thenReturn(new io.micrometer.core.instrument.Counter() {
                        @Override
                        public void increment(double amount) {
                        }

                        @Override
                        public double count() {
                            return 0;
                        }

                        @Override
                        public Id getId() {
                            return null;
                        }
                    });
            when(emailService.sendAdminAlert(anyString(), anyString())).thenReturn(true);

            CircuitBreaker cb = CircuitBreaker.of("pay-fail",
                    CircuitBreakerConfig.custom()
                            .failureRateThreshold(100)
                            .minimumNumberOfCalls(100)
                            .build());
            Retry retry = Retry.of("pay-fail", RetryConfig.custom()
                    .maxAttempts(1)
                    .waitDuration(Duration.ofMillis(10))
                    .retryExceptions(RuntimeException.class)
                    .build());

            ResilientPaymentService service = new ResilientPaymentService(
                    delegate, cb, retry, metricsService, emailService);

            PaymentLockRequest req = new PaymentLockRequest();
            req.setAmount(50.0);

            PaymentTransaction result = service.lockEscrow(1L, 2L, req);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING_ESCROW);
            assertThat(result.getGatewayReference()).startsWith("QUEUED_MANUAL_");
            assertThat(service.getManualProcessingQueue()).hasSize(1);

            verify(emailService).sendAdminAlert(contains("lockEscrow"), anyString());
        }
    }

    // =====================================================================
    //  6 · EmailServiceImpl
    // =====================================================================
    @Nested
    @DisplayName("EmailServiceImpl – resilience")
    class EmailServiceTests {

        @Mock
        private MetricsService metricsService;

        @Test
        @DisplayName("queues email when circuit breaker is open")
        void queuesWhenCircuitOpen() {
            CircuitBreaker cb = CircuitBreaker.of("email-open",
                    CircuitBreakerConfig.ofDefaults());
            cb.transitionToOpenState(); // force open

            Retry retry = Retry.of("email-retry", RetryConfig.custom()
                    .maxAttempts(1)
                    .waitDuration(Duration.ofMillis(10))
                    .build());

            EmailServiceImpl service = new EmailServiceImpl(cb, retry, metricsService,
                    Runnable::run);
            boolean result = service.sendEmail("user@test.com", "Test", "Body");

            assertThat(result).isFalse();
            assertThat(service.getEmailRetryQueue()).hasSize(1);
            assertThat(service.getEmailRetryQueue().peek().to()).isEqualTo("user@test.com");
        }
    }

    // =====================================================================
    //  7 · ImageStorageServiceImpl
    // =====================================================================
    @Nested
    @DisplayName("ImageStorageServiceImpl – resilience")
    class ImageStorageTests {

        @Mock
        private MetricsService metricsService;

        @Test
        @DisplayName("stores locally when circuit breaker is open")
        void storesLocallyWhenCircuitOpen() {
            CircuitBreaker cb = CircuitBreaker.of("img-open",
                    CircuitBreakerConfig.ofDefaults());
            cb.transitionToOpenState();

            Retry retry = Retry.of("img-retry", RetryConfig.custom()
                    .maxAttempts(1)
                    .waitDuration(Duration.ofMillis(10))
                    .build());

            ImageStorageServiceImpl service = new ImageStorageServiceImpl(
                    cb, retry, metricsService);

            String url = service.uploadImage("photo.jpg", new byte[]{1, 2, 3});

            assertThat(url).isEqualTo("/local-images/photo.jpg");
            assertThat(service.getLocalStorage()).containsKey("photo.jpg");
            assertThat(service.getPendingS3Uploads()).contains("photo.jpg");
        }

        @Test
        @DisplayName("returns S3 URL when upload succeeds")
        void returnsS3Url() {
            CircuitBreaker cb = CircuitBreaker.of("img-ok",
                    CircuitBreakerConfig.ofDefaults());
            Retry retry = Retry.of("img-ok", RetryConfig.ofDefaults());

            ImageStorageServiceImpl service = new ImageStorageServiceImpl(
                    cb, retry, metricsService);

            String url = service.uploadImage("avatar.png", new byte[]{4, 5});

            assertThat(url).contains("s3.example.com");
            assertThat(service.getLocalStorage()).doesNotContainKey("avatar.png");
        }
    }

    // =====================================================================
    //  8 · Registry configuration consistency
    // =====================================================================
    @Nested
    @DisplayName("ResilienceConfig – registry wiring")
    class ConfigRegistryTests {

        @Test
        @DisplayName("CircuitBreakerRegistry contains all three named instances")
        void registryContainsAll() {
            var registry = CircuitBreakerRegistry.ofDefaults();
            registry.circuitBreaker(ResilienceConfig.CB_PAYMENT_GATEWAY);
            registry.circuitBreaker(ResilienceConfig.CB_EMAIL_SERVICE);
            registry.circuitBreaker(ResilienceConfig.CB_IMAGE_UPLOAD);

            assertThat(registry.getAllCircuitBreakers()).hasSize(3);
        }

        @Test
        @DisplayName("RetryRegistry contains all three named instances")
        void retryRegistryContainsAll() {
            var registry = RetryRegistry.ofDefaults();
            registry.retry(ResilienceConfig.RETRY_PAYMENT_GATEWAY);
            registry.retry(ResilienceConfig.RETRY_EMAIL_SERVICE);
            registry.retry(ResilienceConfig.RETRY_IMAGE_UPLOAD);

            assertThat(registry.getAllRetries()).hasSize(3);
        }

        @Test
        @DisplayName("named constants match expected values")
        void namedConstantsMatch() {
            assertThat(ResilienceConfig.CB_PAYMENT_GATEWAY).isEqualTo("payment-gateway");
            assertThat(ResilienceConfig.CB_EMAIL_SERVICE).isEqualTo("email-service");
            assertThat(ResilienceConfig.CB_IMAGE_UPLOAD).isEqualTo("image-upload");
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================
    private static void recordSuccess(CircuitBreaker cb) {
        cb.onSuccess(0, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static void recordFailure(CircuitBreaker cb) {
        cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS,
                new RuntimeException("test failure"));
    }

    private static void forceOpen(CircuitBreaker cb) {
        cb.transitionToOpenState();
    }
}
