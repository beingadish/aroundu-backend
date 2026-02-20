package com.beingadish.AroundU.Config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * In the <code>test</code> profile, provides a mock {@link ProxyManager} so no
 * Redis connection is needed. Rate limiting is disabled via property
 * {@code rate-limit.enabled=false} in application-test.yml.
 */
@Configuration
@Profile("test")
public class TestRateLimitConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ProxyManager<String> proxyManager() {
        return Mockito.mock(ProxyManager.class);
    }
}
