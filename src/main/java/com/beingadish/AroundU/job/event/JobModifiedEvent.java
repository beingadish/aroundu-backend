package com.beingadish.AroundU.job.event;

/**
 * Published by {@link com.beingadish.AroundU.job.service.impl.JobServiceImpl}
 * whenever a job is created, updated, or deleted. Listeners handle cache
 * eviction and geo-index maintenance after the transaction commits.
 *
 * @param locationChanged {@code true} when the job's physical location was
 * changed (relevant for geo-index / worker-feed cache).
 */
public record JobModifiedEvent(Long jobId, Long clientId, Type type, boolean locationChanged) {

    public enum Type {
        CREATED,
        UPDATED,
        STATUS_CHANGED,
        DELETED
    }
}
