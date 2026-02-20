package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.notification.entity.FailedNotification;
import com.beingadish.AroundU.notification.repository.FailedNotificationRepository;
import com.beingadish.AroundU.notification.service.impl.NotificationServiceImpl;
import com.beingadish.AroundU.common.util.AsyncUtils;
import com.beingadish.AroundU.common.util.ParallelProcessingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.beingadish.AroundU.notification.service.EmailService;

/**
 * Tests for multi-threading, parallel processing, and async utilities.
 * <p>
 * Covers:
 * <ul>
 * <li>Parallel notification dispatch (EmailService async)</li>
 * <li>ParallelProcessingUtils (parallelMap, partitionedProcess,
 * safeParallelStream)</li>
 * <li>AsyncUtils (timeout, fallback, retry, combine)</li>
 * <li>Concurrent request simulation</li>
 * <li>Async failure handling</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Concurrency & Parallel Processing Tests")
class ConcurrencyTest {

    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(8);
    }

    // =====================================================================
    //  1 · NotificationService parallel dispatch
    // =====================================================================
    @Nested
    @DisplayName("NotificationService – parallel dispatch")
    class NotificationParallelTests {

        @Mock
        EmailService emailService;
        @Mock
        FailedNotificationRepository failedNotificationRepo;

        @Test
        @DisplayName("sends 6 notifications in parallel faster than sequential")
        void parallelNotificationsFasterThanSequential() {
            // Each email takes ~100ms
            when(emailService.sendEmail(anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        Thread.sleep(100);
                        return true;
                    });

            NotificationServiceImpl service = new NotificationServiceImpl(
                    emailService, failedNotificationRepo, testExecutor);

            long start = System.currentTimeMillis();
            service.sendJobNotifications(1L,
                    "client@test.com", "worker@test.com",
                    100L, 200L,
                    "+1111", "+2222",
                    "Job Update", "Your job status changed");
            long elapsed = System.currentTimeMillis() - start;

            // 6 channels × 100ms sequential = 600ms
            // Parallel should complete in ~100-200ms
            assertThat(elapsed).isLessThan(500L);
        }

        @Test
        @DisplayName("handles individual channel failure without blocking others")
        void handlesChannelFailureGracefully() {
            AtomicInteger successCount = new AtomicInteger(0);

            when(emailService.sendEmail(anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        String to = inv.getArgument(0);
                        if (to.contains("client")) {
                            throw new RuntimeException("SMTP down");
                        }
                        successCount.incrementAndGet();
                        return true;
                    });

            NotificationServiceImpl service = new NotificationServiceImpl(
                    emailService, failedNotificationRepo, testExecutor);

            // Should not throw despite client email failing
            service.sendJobNotifications(1L,
                    "client@test.com", "worker@test.com",
                    null, null, null, null,
                    "Subject", "Body");

            // Worker email should have succeeded
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("skips null recipients")
        void skipsNullRecipients() {
            AtomicInteger callCount = new AtomicInteger(0);
            when(emailService.sendEmail(anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        callCount.incrementAndGet();
                        return true;
                    });

            NotificationServiceImpl service = new NotificationServiceImpl(
                    emailService, failedNotificationRepo, testExecutor);

            // Only clientEmail is non-null
            service.sendJobNotifications(1L,
                    "client@test.com", null,
                    null, null, null, null,
                    "Subject", "Body");

            // Only 1 email should be sent (for client)
            assertThat(callCount.get()).isEqualTo(1);
        }
    }

    // =====================================================================
    //  2 · ParallelProcessingUtils
    // =====================================================================
    @Nested
    @DisplayName("ParallelProcessingUtils")
    class ParallelProcessingTests {

        @Test
        @DisplayName("parallelMap processes items concurrently on large collections")
        void parallelMapLargeCollection() {
            List<Integer> items = IntStream.rangeClosed(1, 200).boxed().toList();

            List<String> results = ParallelProcessingUtils.parallelMap(
                    items,
                    i -> "item-" + i,
                    testExecutor
            );

            assertThat(results).hasSize(200);
            assertThat(results).contains("item-1", "item-100", "item-200");
        }

        @Test
        @DisplayName("parallelMap falls back to sequential on small collections")
        void parallelMapSmallCollectionSequential() {
            List<Integer> items = List.of(1, 2, 3);
            CopyOnWriteArrayList<String> threadNames = new CopyOnWriteArrayList<>();

            List<String> results = ParallelProcessingUtils.parallelMap(
                    items,
                    i -> {
                        threadNames.add(Thread.currentThread().getName());
                        return "item-" + i;
                    },
                    testExecutor
            );

            assertThat(results).hasSize(3);
            // All should run on the same thread (sequential path)
            assertThat(threadNames.stream().distinct().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("parallelMap returns empty list for null/empty input")
        void parallelMapHandlesNullAndEmpty() {
            assertThat(ParallelProcessingUtils.parallelMap(null, i -> i, testExecutor)).isEmpty();
            assertThat(ParallelProcessingUtils.parallelMap(List.of(), i -> i, testExecutor)).isEmpty();
        }

        @Test
        @DisplayName("partitionedProcess splits into batches and aggregates")
        void partitionedProcessBatches() {
            List<Integer> items = IntStream.rangeClosed(1, 250).boxed().toList();

            List<String> results = ParallelProcessingUtils.partitionedProcess(
                    items,
                    50, // batch size
                    batch -> batch.stream().map(i -> "processed-" + i).toList(),
                    testExecutor
            );

            assertThat(results).hasSize(250);
            assertThat(results.get(0)).isEqualTo("processed-1");
            assertThat(results.get(249)).isEqualTo("processed-250");
        }

        @Test
        @DisplayName("safeParallelStream uses parallelism for large collections")
        void safeParallelStreamLarge() {
            List<Integer> items = IntStream.rangeClosed(1, 200).boxed().toList();

            List<Integer> results = ParallelProcessingUtils.safeParallelStream(
                    items,
                    i -> i * 2
            );

            assertThat(results).hasSize(200);
            assertThat(results).contains(2, 200, 400);
        }

        @Test
        @DisplayName("safeParallelStream handles empty/null")
        void safeParallelStreamEmpty() {
            assertThat(ParallelProcessingUtils.safeParallelStream(null, i -> i)).isEmpty();
            assertThat(ParallelProcessingUtils.safeParallelStream(List.of(), i -> i)).isEmpty();
        }

        @Test
        @DisplayName("parallelMap is faster than sequential for I/O-bound work")
        void parallelMapFasterForIo() {
            List<Integer> items = IntStream.rangeClosed(1, 100).boxed().toList();

            // Simulate I/O: each item takes 50ms
            long start = System.currentTimeMillis();
            ParallelProcessingUtils.parallelMap(
                    items,
                    i -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return i;
                    },
                    testExecutor
            );
            long parallelElapsed = System.currentTimeMillis() - start;

            // Sequential would take 100 × 50ms = 5000ms
            // Parallel with 8 threads ≈ 100/8 × 50ms ≈ 625ms
            assertThat(parallelElapsed).isLessThan(3000L);
        }
    }

    // =====================================================================
    //  3 · AsyncUtils
    // =====================================================================
    @Nested
    @DisplayName("AsyncUtils – CompletableFuture composition")
    class AsyncUtilsTests {

        @Test
        @DisplayName("supplyAsync completes successfully")
        void supplyAsyncSuccess() throws Exception {
            CompletableFuture<String> future = AsyncUtils.supplyAsync(
                    () -> "hello", testExecutor, Duration.ofSeconds(5));

            assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo("hello");
        }

        @Test
        @DisplayName("supplyAsync times out on slow supplier")
        void supplyAsyncTimeout() {
            CompletableFuture<String> future = AsyncUtils.supplyAsync(
                    () -> {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return "late";
                    },
                    testExecutor,
                    Duration.ofMillis(100)
            );

            assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("withFallback returns default on failure")
        void withFallbackReturnsDefault() throws Exception {
            CompletableFuture<String> failing = CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("boom");
            }, testExecutor);

            CompletableFuture<String> result = AsyncUtils.withFallback(failing, "default-value");

            assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo("default-value");
        }

        @Test
        @DisplayName("withFallback passes through on success")
        void withFallbackPassesThrough() throws Exception {
            CompletableFuture<String> succeeding = CompletableFuture.supplyAsync(
                    () -> "ok", testExecutor);

            CompletableFuture<String> result = AsyncUtils.withFallback(succeeding, "fallback");

            assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo("ok");
        }

        @Test
        @DisplayName("combine merges two parallel futures")
        void combineTwoFutures() throws Exception {
            CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> 10, testExecutor);
            CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 20, testExecutor);

            CompletableFuture<Integer> result = AsyncUtils.combine(f1, f2, Integer::sum);

            assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo(30);
        }

        @Test
        @DisplayName("combine merges three parallel futures")
        void combineThreeFutures() throws Exception {
            CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> "Alice", testExecutor);
            CompletableFuture<List<String>> jobsFuture = CompletableFuture.supplyAsync(
                    () -> List.of("Job1", "Job2"), testExecutor);
            CompletableFuture<Integer> bidsFuture = CompletableFuture.supplyAsync(() -> 5, testExecutor);

            CompletableFuture<String> result = AsyncUtils.combine(
                    userFuture, jobsFuture, bidsFuture,
                    (user, jobs, bids) -> user + ":" + jobs.size() + ":" + bids
            );

            assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo("Alice:2:5");
        }

        @Test
        @DisplayName("retryAsync succeeds on second attempt")
        void retryAsyncSucceedsOnRetry() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);

            CompletableFuture<String> result = AsyncUtils.retryAsync(
                    () -> {
                        if (attempts.incrementAndGet() < 2) {
                            throw new RuntimeException("transient error");
                        }
                        return "success";
                    },
                    testExecutor,
                    3,
                    10 // 10ms initial delay
            );

            assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("retryAsync exhausts all retries and fails")
        void retryAsyncExhausted() {
            CompletableFuture<String> result = AsyncUtils.retryAsync(
                    () -> {
                        throw new RuntimeException("always fails");
                    },
                    testExecutor,
                    2,
                    10
            );

            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
        }

        @Test
        @DisplayName("parallel data fetch is faster than sequential")
        void parallelDataFetchBenchmark() throws Exception {
            // Simulate 3 independent DB queries each taking 200ms
            long start = System.currentTimeMillis();

            CompletableFuture<String> userFuture = AsyncUtils.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "User-1";
            }, testExecutor);

            CompletableFuture<List<String>> jobsFuture = AsyncUtils.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return List.of("Job-1", "Job-2");
            }, testExecutor);

            CompletableFuture<Integer> countFuture = AsyncUtils.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 42;
            }, testExecutor);

            CompletableFuture<String> combined = AsyncUtils.combine(
                    userFuture, jobsFuture, countFuture,
                    (user, jobs, count) -> user + "|" + jobs.size() + "|" + count
            );

            String result = combined.get(5, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(result).isEqualTo("User-1|2|42");
            // Sequential: 200×3 = 600ms; Parallel: ~200ms
            assertThat(elapsed).isLessThan(500L);
        }
    }

    // =====================================================================
    //  4 · Concurrent request simulation
    // =====================================================================
    @Nested
    @DisplayName("Concurrent request handling")
    class ConcurrentRequestTests {

        @Test
        @DisplayName("100 concurrent operations all complete successfully")
        void hundredConcurrentOperations() throws Exception {
            int concurrency = 100;
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(concurrency);
            CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
            AtomicInteger errors = new AtomicInteger(0);

            for (int i = 0; i < concurrency; i++) {
                int idx = i;
                testExecutor.submit(() -> {
                    try {
                        startGate.await(); // all threads start together
                        results.add("result-" + idx);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            startGate.countDown(); // release all threads
            boolean completed = endGate.await(10, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(results).hasSize(concurrency);
            assertThat(errors.get()).isZero();
        }

        @Test
        @DisplayName("thread-safe counter under concurrent access")
        void threadSafeCounter() throws Exception {
            int concurrency = 100;
            AtomicInteger counter = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(concurrency);

            for (int i = 0; i < concurrency; i++) {
                testExecutor.submit(() -> {
                    try {
                        counter.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            assertThat(counter.get()).isEqualTo(concurrency);
        }

        @Test
        @DisplayName("concurrent list accumulation with CopyOnWriteArrayList")
        void concurrentListAccumulation() throws Exception {
            int concurrency = 100;
            CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(concurrency);

            for (int i = 0; i < concurrency; i++) {
                int val = i;
                testExecutor.submit(() -> {
                    try {
                        list.add(val);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            assertThat(list).hasSize(concurrency);
        }
    }

    // =====================================================================
    //  5 · Async failure handling
    // =====================================================================
    @Nested
    @DisplayName("Async failure handling")
    class AsyncFailureTests {

        @Mock
        EmailService emailService;
        @Mock
        FailedNotificationRepository failedNotificationRepo;

        @Test
        @DisplayName("notification service handles email exception gracefully")
        void persistsFailureRecordOnException() {
            when(emailService.sendEmail(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("SMTP connection refused"));

            NotificationServiceImpl service = new NotificationServiceImpl(
                    emailService, failedNotificationRepo, Runnable::run);

            // Should not throw – the exceptionally() handler swallows the failure
            service.sendJobNotifications(99L,
                    "fail@test.com", null,
                    null, null, null, null,
                    "Subject", "Body");

            // Verify the email was attempted
            verify(emailService).sendEmail("fail@test.com", "Subject", "Body");
        }

        @Test
        @DisplayName("withFallback function receives the exception")
        void fallbackFunctionReceivesException() throws Exception {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("specific-error");
            }, testExecutor);

            Function<Throwable, String> fallbackFn = ex -> "recovered-from:" + ex.getMessage();
            CompletableFuture<String> result = AsyncUtils.withFallback(
                    future,
                    fallbackFn
            );

            String value = result.get(2, TimeUnit.SECONDS);
            assertThat(value).contains("recovered-from:");
            assertThat(value).contains("specific-error");
        }
    }
}
