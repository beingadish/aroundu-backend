package com.beingadish.AroundU.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Test-profile replacement for {@link ThreadPoolConfig}. Provides synchronous
 * executors so all {@code @Async} and {@code CompletableFuture.supplyAsync}
 * calls execute on the calling thread, making tests deterministic and avoiding
 * thread-pool overhead.
 */
@Configuration
@Profile("test")
public class TestThreadPoolConfig {

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("databaseExecutor")
    public Executor databaseExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("apiExecutor")
    public Executor apiExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("computationExecutor")
    public Executor computationExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return new SyncTaskExecutor();
    }
}
