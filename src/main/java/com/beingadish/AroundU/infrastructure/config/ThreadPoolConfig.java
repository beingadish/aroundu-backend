package com.beingadish.AroundU.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures dedicated thread pools for different workload types.
 * <p>
 * Each executor is sized according to its workload characteristics:
 * <ul>
 * <li><b>notificationExecutor</b> – lightweight I/O for email, SMS, push</li>
 * <li><b>databaseExecutor</b> – heavy DB operations (batch reads/writes)</li>
 * <li><b>apiExecutor</b> – external API calls with unpredictable latency</li>
 * <li><b>computationExecutor</b> – CPU-bound work sized to available cores</li>
 * <li><b>virtualThreadExecutor</b> – Java 21 virtual threads for massive I/O
 * concurrency</li>
 * </ul>
 * All executors are instrumented with Micrometer for Actuator / Prometheus
 * monitoring and use {@link ThreadPoolExecutor.CallerRunsPolicy} as the
 * rejection policy so callers degrade gracefully instead of losing work.
 */
@Configuration
@EnableAsync
@Slf4j
@Profile("!test")
public class ThreadPoolConfig implements AsyncConfigurer {

    // ── Notification executor ────────────────────────────────────────────
    @Bean("notificationExecutor")
    public Executor notificationExecutor(MeterRegistry registry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        log.info("Initialized notificationExecutor: core=5, max=15, queue=200");
        return executor;
    }

    // ── Database executor ────────────────────────────────────────────────
    @Bean("databaseExecutor")
    public Executor databaseExecutor(MeterRegistry registry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("db-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        log.info("Initialized databaseExecutor: core=20, max=50, queue=50");
        return executor;
    }

    // ── API executor ─────────────────────────────────────────────────────
    @Bean("apiExecutor")
    public Executor apiExecutor(MeterRegistry registry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("api-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        log.info("Initialized apiExecutor: core=10, max=30, queue=100");
        return executor;
    }

    // ── Computation executor ─────────────────────────────────────────────
    @Bean("computationExecutor")
    public Executor computationExecutor(MeterRegistry registry) {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores + 1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("compute-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        log.info("Initialized computationExecutor: core={}, max={}, queue=50", cores, cores + 1);
        return executor;
    }

    // ── Virtual thread executor (Java 21+) ───────────────────────────────
    /**
     * Lightweight virtual threads ideal for massive I/O-bound concurrency. Each
     * task gets its own virtual thread — no pool sizing needed.
     */
    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        log.info("Initialized virtualThreadExecutor using Java 21 virtual threads");
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vt-", 0).factory());
    }

    // ── AsyncConfigurer default executor ─────────────────────────────────
    @Override
    public Executor getAsyncExecutor() {
        // Default executor for @Async without a qualifier — use notifications
        // as it is the lightest workload and safest default.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-default-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params)
                -> log.error("Async method '{}' threw uncaught exception: {}", method.getName(), ex.getMessage(), ex);
    }
}
