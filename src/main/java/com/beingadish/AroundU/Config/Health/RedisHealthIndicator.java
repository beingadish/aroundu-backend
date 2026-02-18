package com.beingadish.AroundU.Config.Health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator that checks Redis connectivity. Reports UP when a
 * PING to Redis succeeds, DOWN otherwise.
 * <p>
 * Only registered when a {@link RedisConnectionFactory} bean is available (i.e.
 * not in the "test" profile where Redis auto-configuration is excluded).
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "Available")
                        .build();
            }
            return Health.down()
                    .withDetail("redis", "Unexpected PING response: " + pong)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
