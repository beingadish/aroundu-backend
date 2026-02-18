package com.beingadish.AroundU.Events;

/**
 * Published when a job is automatically closed because its scheduled start time
 * (or age threshold) has passed. Listeners can send notifications to the client
 * and any interested workers.
 */
public record JobExpiredEvent(Long jobId, Long clientId) {

}
