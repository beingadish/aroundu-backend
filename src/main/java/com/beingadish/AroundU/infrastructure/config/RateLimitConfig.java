package com.beingadish.AroundU.infrastructure.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * Configures Bucket4j's Redis-backed {@link ProxyManager} for distributed rate
 * limiting.
 * <p>
 * Rate-limit buckets are persisted in Redis via Redisson, so:
 * <ul>
 * <li>Limits survive application restarts</li>
 * <li>Multiple application instances share the same counters</li>
 * </ul>
 * Disabled in the <code>test</code> profile – see {@code TestRateLimitConfig}.
 */
@Configuration
@Profile("!test")
@Slf4j
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redissonClient) {
        CommandAsyncExecutor commandExecutor = extractCommandExecutor(redissonClient);
        ProxyManager<String> manager = RedissonBasedProxyManager.builderFor(commandExecutor)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1)))
                .build();
        log.info("Bucket4j ProxyManager initialized with Redisson backend");
        return manager;
    }

    /**
     * Extracts the {@link CommandAsyncExecutor} from the Redisson client.
     * Redisson exposes this via {@code getCommandExecutor()} on its concrete
     * class.
     */
    private CommandAsyncExecutor extractCommandExecutor(RedissonClient redissonClient) {
        if (redissonClient instanceof org.redisson.Redisson redisson) {
            return redisson.getCommandExecutor();
        }
        throw new IllegalStateException(
                "Cannot extract CommandAsyncExecutor: RedissonClient is not an instance of org.redisson.Redisson");
    }
}
