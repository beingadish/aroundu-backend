package com.beingadish.AroundU.Events;

/**
 * Published by {@link com.beingadish.AroundU.Service.impl.JobServiceImpl}
 * whenever a job is created, updated, or deleted. Listeners handle cache
 * eviction and geo-index maintenance after the transaction commits.
 */
public record JobModifiedEvent(Long jobId, Long clientId, Type type) {

    public enum Type {
        CREATED,
        UPDATED,
        STATUS_CHANGED,
        DELETED
    }
}
