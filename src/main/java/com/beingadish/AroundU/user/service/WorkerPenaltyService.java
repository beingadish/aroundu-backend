package com.beingadish.AroundU.user.service;

import com.beingadish.AroundU.user.entity.Worker;

/**
 * Manages worker cancellation penalties.
 * <p>
 * When a worker cancels an accepted / in-progress job:
 * <ul>
 * <li>Their cancellation count is incremented.</li>
 * <li>If the count reaches the threshold (default 3), they are blocked from
 * accepting new jobs for a configurable period (default 7 days).</li>
 * </ul>
 */
public interface WorkerPenaltyService {

    /**
     * Records a cancellation and applies a block if the threshold is reached.
     *
     * @param workerId the worker who cancelled
     * @return the updated Worker entity
     */
    Worker recordCancellation(Long workerId);

    /**
     * Checks whether a worker is currently blocked.
     */
    boolean isBlocked(Long workerId);

    /**
     * Unblocks workers whose block period has expired. Called by scheduler.
     */
    void unblockExpiredWorkers();
}
