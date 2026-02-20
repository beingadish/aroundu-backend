package com.beingadish.AroundU.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper methods for composing {@link CompletableFuture} pipelines.
 * <p>
 * Provides:
 * <ul>
 * <li>Timeout-aware async execution</li>
 * <li>Fallback composition on failure</li>
 * <li>Parallel fetch of independent data with combined result</li>
 * <li>Retry with exponential back-off</li>
 * </ul>
 *
 * <b>Usage example — parallel profile loading:</b>
 * <pre>
 * CompletableFuture&lt;UserDTO&gt; userFuture =
 *     AsyncUtils.supplyAsync(() -&gt; userRepo.findById(id), dbExecutor, Duration.ofSeconds(5));
 *
 * CompletableFuture&lt;List&lt;JobDTO&gt;&gt; jobsFuture =
 *     AsyncUtils.supplyAsync(() -&gt; jobRepo.findByClientId(id), dbExecutor, Duration.ofSeconds(5));
 *
 * CompletableFuture&lt;ProfileDTO&gt; profile =
 *     CompletableFuture.allOf(userFuture, jobsFuture)
 *         .thenApply(v -&gt; new ProfileDTO(userFuture.join(), jobsFuture.join()));
 * </pre>
 */
@Slf4j
public final class AsyncUtils {

    private AsyncUtils() {
        // utility class
    }

    // ── Async with timeout ───────────────────────────────────────────────
    /**
     * Runs a supplier asynchronously on the given executor, completing
     * exceptionally if the timeout elapses.
     *
     * @param supplier the computation
     * @param executor the thread pool to run on
     * @param timeout maximum duration to wait
     * @param <T> result type
     * @return a CompletableFuture that times out if the supplier is too slow
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier,
            Executor executor,
            Duration timeout) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Runs a supplier asynchronously with a default 10-second timeout.
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier,
            Executor executor) {
        return supplyAsync(supplier, executor, Duration.ofSeconds(10));
    }

    // ── Fallback ─────────────────────────────────────────────────────────
    /**
     * Returns a future that falls back to the given default value on failure.
     *
     * @param future the primary future
     * @param fallback the default value on failure
     * @param <T> result type
     * @return a future that never completes exceptionally
     */
    public static <T> CompletableFuture<T> withFallback(CompletableFuture<T> future,
            T fallback) {
        return future.exceptionally(ex -> {
            log.warn("CompletableFuture failed, returning fallback: {}", ex.getMessage());
            return fallback;
        });
    }

    /**
     * Returns a future that falls back to a computed value on failure.
     */
    public static <T> CompletableFuture<T> withFallback(CompletableFuture<T> future,
            Function<Throwable, T> fallbackFn) {
        return future.exceptionally(ex -> {
            log.warn("CompletableFuture failed, computing fallback: {}", ex.getMessage());
            return fallbackFn.apply(ex);
        });
    }

    // ── Retry ────────────────────────────────────────────────────────────
    /**
     * Retries an async supplier up to {@code maxRetries} times with exponential
     * back-off. The initial delay doubles on each retry.
     *
     * @param supplier the computation to retry
     * @param executor thread pool
     * @param maxRetries maximum number of retry attempts
     * @param initialDelayMs initial delay between retries in milliseconds
     * @param <T> result type
     * @return a future that succeeds if any attempt succeeds
     */
    public static <T> CompletableFuture<T> retryAsync(Supplier<T> supplier,
            Executor executor,
            int maxRetries,
            long initialDelayMs) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .thenApply(CompletableFuture::completedFuture)
                .exceptionally(ex -> retryInternal(supplier, executor, maxRetries, 1, initialDelayMs, ex))
                .thenCompose(Function.identity());
    }

    private static <T> CompletableFuture<T> retryInternal(Supplier<T> supplier,
            Executor executor,
            int maxRetries,
            int attempt,
            long delayMs,
            Throwable lastError) {
        if (attempt > maxRetries) {
            log.error("All {} retry attempts exhausted: {}", maxRetries, lastError.getMessage());
            return CompletableFuture.failedFuture(lastError);
        }

        log.warn("Retry attempt {}/{} after {}ms: {}", attempt, maxRetries, delayMs, lastError.getMessage());

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return supplier.get();
        }, executor).thenApply(CompletableFuture::completedFuture)
                .exceptionally(ex -> retryInternal(supplier, executor, maxRetries, attempt + 1, delayMs * 2, ex))
                .thenCompose(Function.identity());
    }

    // ── Combine two futures ──────────────────────────────────────────────
    /**
     * Combines two independent futures into a single result using the combiner
     * function. Both futures run in parallel.
     */
    public static <A, B, R> CompletableFuture<R> combine(CompletableFuture<A> futureA,
            CompletableFuture<B> futureB,
            java.util.function.BiFunction<A, B, R> combiner) {
        return futureA.thenCombine(futureB, combiner);
    }

    /**
     * Combines three independent futures into a single result.
     */
    public static <A, B, C, R> CompletableFuture<R> combine(CompletableFuture<A> futA,
            CompletableFuture<B> futB,
            CompletableFuture<C> futC,
            TriFunction<A, B, C, R> combiner) {
        return CompletableFuture.allOf(futA, futB, futC)
                .thenApply(v -> combiner.apply(futA.join(), futB.join(), futC.join()));
    }

    /**
     * Functional interface for a three-argument function.
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {

        R apply(A a, B b, C c);
    }
}
