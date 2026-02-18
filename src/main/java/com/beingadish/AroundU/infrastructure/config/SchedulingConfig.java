package com.beingadish.AroundU.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Enables {@link org.springframework.scheduling.annotation.Scheduled} support
 * for non-test profiles. Configures a dedicated thread pool for scheduled tasks
 * and a separate executor for long-running background work.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {

    /**
     * Scheduler thread pool for {@code @Scheduled} methods. Ten threads allow
     * multiple tasks to execute concurrently without blocking each other.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setErrorHandler(t
                -> org.slf4j.LoggerFactory.getLogger("SchedulerErrorHandler")
                        .error("Scheduled task failed", t));
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Virtual-thread executor for long-running background tasks (analytics
     * aggregation, large batch cleanups) that should not block the scheduler
     * pool.
     */
    @Bean("longRunningExecutor")
    public Executor longRunningExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("bg-task-", 0).factory());
    }
}
