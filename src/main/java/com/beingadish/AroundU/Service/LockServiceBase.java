package com.beingadish.AroundU.Service;

import java.time.Duration;

/**
 * Base abstraction for the distributed lock service, allowing a Redis-backed
 * implementation in production and a no-op variant in tests.
 */
public abstract class LockServiceBase {

    public abstract boolean tryAcquireLock(String taskName, Duration ttl);

    public abstract void releaseLock(String taskName);
}
