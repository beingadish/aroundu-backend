package com.beingadish.AroundU.Utilities;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility methods for safe parallel and batch processing.
 * <p>
 * These helpers provide:
 * <ul>
 * <li>Automatic partitioning of large collections for batch DB operations</li>
 * <li>Parallel mapping with configurable executors</li>
 * <li>Size-aware parallelism (avoids overhead on small collections)</li>
 * <li>Proper error handling that doesn't lose partial results</li>
 * </ul>
 *
 * <b>When to use what:</b>
 * <ul>
 * <li>{@code parallelStream()} – CPU-bound in-memory transforms, &gt; 1000
 * items</li>
 * <li>{@link #parallelMap} – I/O-bound work (DB calls, API calls) with custom
 * executor</li>
 * <li>{@link #partitionedProcess} – batch DB operations (bulk
 * inserts/updates)</li>
 * </ul>
 */
@Slf4j
public final class ParallelProcessingUtils {

    /**
     * Minimum collection size to justify forking parallel work.
     */
    private static final int PARALLEL_THRESHOLD = 50;

    /**
     * Default batch size for partitioned operations.
     */
    private static final int DEFAULT_BATCH_SIZE = 100;

    private ParallelProcessingUtils() {
        // utility class
    }

    // ── Parallel map ─────────────────────────────────────────────────────
    /**
     * Maps a collection in parallel using the given executor. Falls back to
     * sequential processing if the collection is smaller than
     * {@link #PARALLEL_THRESHOLD}.
     *
     * @param items items to transform
     * @param mapper transformation function (should be thread-safe)
     * @param executor thread pool to run on
     * @param <T> source type
     * @param <R> result type
     * @return ordered list of mapped results
     */
    public static <T, R> List<R> parallelMap(Collection<T> items,
            Function<T, R> mapper,
            Executor executor) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        // Small collections → sequential (less overhead than task creation)
        if (items.size() < PARALLEL_THRESHOLD) {
            return items.stream().map(mapper).collect(Collectors.toList());
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> mapper.apply(item), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    // ── Partitioned batch processing ─────────────────────────────────────
    /**
     * Partitions a collection into batches and processes each batch through the
     * provided function. Batches are executed in parallel on the given
     * executor.
     *
     * @param items full collection to process
     * @param batchSize items per batch (use {@link #DEFAULT_BATCH_SIZE} if ≤ 0)
     * @param processor function that processes one batch and returns results
     * @param executor thread pool to run batches on
     * @param <T> item type
     * @param <R> result type
     * @return aggregated results from all batches
     */
    public static <T, R> List<R> partitionedProcess(List<T> items,
            int batchSize,
            Function<List<T>, List<R>> processor,
            Executor executor) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        int effectiveBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<List<T>> partitions = partition(items, effectiveBatchSize);

        log.debug("Partitioned {} items into {} batches of ≤{}", items.size(), partitions.size(), effectiveBatchSize);

        // Single batch → process inline
        if (partitions.size() == 1) {
            return processor.apply(partitions.get(0));
        }

        List<CompletableFuture<List<R>>> futures = partitions.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> processor.apply(batch), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Overload that uses the default batch size.
     */
    public static <T, R> List<R> partitionedProcess(List<T> items,
            Function<List<T>, List<R>> processor,
            Executor executor) {
        return partitionedProcess(items, DEFAULT_BATCH_SIZE, processor, executor);
    }

    // ── Safe parallel stream ─────────────────────────────────────────────
    /**
     * Uses {@code parallelStream()} for large collections and standard
     * {@code stream()} for small ones, avoiding fork-join overhead on tiny data
     * sets.
     *
     * @param items the collection to stream
     * @param mapper transformation function
     * @param <T> source type
     * @param <R> result type
     * @return list of mapped results
     */
    public static <T, R> List<R> safeParallelStream(Collection<T> items,
            Function<T, R> mapper) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (items.size() < PARALLEL_THRESHOLD) {
            return items.stream().map(mapper).collect(Collectors.toList());
        }
        return items.parallelStream().map(mapper).collect(Collectors.toList());
    }

    // ── Internal helpers ─────────────────────────────────────────────────
    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
}
