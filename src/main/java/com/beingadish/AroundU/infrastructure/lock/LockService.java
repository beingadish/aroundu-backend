package com.beingadish.AroundU.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based distributed lock service. In a single-instance deployment the
 * lock is technically redundant, but it future-proofs the system for
 * multi-instance scaling where duplicate scheduled-task execution must be
 * prevented.
 * <p>
 * Lock key format: {@code scheduler:lock:{taskName}}<br>
 * TTL: task max duration + 60-second safety buffer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class LockService extends LockServiceBase {

    private static final String KEY_PREFIX = "scheduler:lock:";
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Attempt to acquire a distributed lock for the given task.
     *
     * @param taskName unique task identifier (e.g. "cleanup-users")
     * @param ttl maximum expected duration + buffer
     * @return {@code true} if the lock was acquired, {@code false} if another
     * instance already holds it
     */
    public boolean tryAcquireLock(String taskName, Duration ttl) {
        String key = KEY_PREFIX + taskName;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "locked", ttl);
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired lock for task={} ttl={}", taskName, ttl);
                return true;
            }
            log.debug("Lock already held for task={}", taskName);
            return false;
        } catch (Exception ex) {
            log.warn("Failed to acquire lock for task={}: {}", taskName, ex.getMessage());
            return false;
        }
    }

    /**
     * Release the lock for the given task. Safe to call even if the lock was
     * never acquired or has already expired.
     */
    public void releaseLock(String taskName) {
        String key = KEY_PREFIX + taskName;
        try {
            stringRedisTemplate.delete(key);
            log.debug("Released lock for task={}", taskName);
        } catch (Exception ex) {
            log.warn("Failed to release lock for task={}: {}", taskName, ex.getMessage());
        }
    }
}
