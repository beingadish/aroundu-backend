package com.beingadish.AroundU.Service;

/**
 * Exposes Bloom filter health and performance metrics via Micrometer/Actuator.
 * <p>
 * Tracks:
 * <ul>
 * <li>Estimated element count per filter</li>
 * <li>False positive occurrences (for analysis)</li>
 * <li>Capacity utilisation alerts (&gt;80%)</li>
 * </ul>
 */
public interface BloomFilterMetricsService {

    /**
     * Record a false-positive occurrence for the named filter.
     */
    void recordFalsePositive(String filterName);

    /**
     * Check all filters and log warnings if any exceed 80% capacity.
     */
    void checkCapacityThresholds();
}
