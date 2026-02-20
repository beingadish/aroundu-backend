package com.beingadish.AroundU.bid.service;

/**
 * Bloom-filter-backed service for efficient duplicate bid detection.
 * <p>
 * Uses a probabilistic check first (O(1)), then falls back to the database only
 * when the Bloom filter reports "possibly present".
 */
public interface BidDuplicateCheckService {

    /**
     * Validate that a worker has not already bid on a job.
     *
     * @param workerId the worker attempting to place a bid
     * @param jobId the job being bid on
     * @throws com.beingadish.AroundU.bid.exception.DuplicateBidException if a
     * duplicate bid is detected
     */
    void validateNoDuplicateBid(Long workerId, Long jobId);

    /**
     * Record a successful bid in the Bloom filter.
     *
     * @param workerId the worker who placed the bid
     * @param jobId the job that was bid on
     */
    void recordBid(Long workerId, Long jobId);
}
