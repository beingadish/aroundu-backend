package com.beingadish.AroundU.infrastructure.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * No-op lock service used in test profile where Redis is unavailable. Always
 * grants the lock so schedulers can be tested via direct invocation.
 */
@Service
@Slf4j
@Profile("test")
public class NoOpLockService extends LockServiceBase {

    @Override
    public boolean tryAcquireLock(String taskName, Duration ttl) {
        log.debug("NoOp lock acquired for task={}", taskName);
        return true;
    }

    @Override
    public void releaseLock(String taskName) {
        log.debug("NoOp lock released for task={}", taskName);
    }
}
