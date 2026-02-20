package com.beingadish.AroundU.infrastructure.cache;

/**
 * Abstracts granular cache eviction so that production profiles use
 * Redis-backed scanning while test profiles use a no-op implementation.
 */
public interface CacheEvictionService {

    /**
     * Evict the cached detail for a single job.
     */
    void evictJobDetail(Long jobId);

    /**
     * Evict all cached client-job list entries for the given client
     * (pattern-based).
     */
    void evictClientJobsCaches(Long clientId);

    /**
     * Evict all cached worker-feed entries.
     */
    void evictWorkerFeedCaches();
}
