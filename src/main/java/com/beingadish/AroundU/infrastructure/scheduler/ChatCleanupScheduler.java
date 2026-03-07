package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.chat.service.ChatService;
import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Daily cleanup of chat conversations.
 * <ol>
 * <li>Archives conversations whose jobs reached a terminal state (COMPLETED /
 * CANCELLED).</li>
 * <li>Deletes conversations that have been archived for more than 30 days.</li>
 * </ol>
 * Default schedule: every day at 03:00 AM.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ChatCleanupScheduler {

    private static final String TASK_NAME = "cleanup-chats";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final LockServiceBase lockService;
    private final ChatService chatService;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;

    @Scheduled(cron = "${scheduler.chat-cleanup-cron:0 0 3 * * ?}")
    public void cleanupChats() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            chatService.archiveCompletedConversations();
            chatService.deleteExpiredConversations();

            long durationMs = System.currentTimeMillis() - start;
            log.info("Chat cleanup completed ({}ms)", durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("Chat cleanup failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }
}
