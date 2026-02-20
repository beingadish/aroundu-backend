package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Daily cleanup of users who haven't logged in for {@code userInactiveYears}
 * (default 2). Soft-deletes and anonymises PII so the row can be retained for
 * referential integrity.
 * <p>
 * Default schedule: every day at 02:00 AM.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private static final String TASK_NAME = "cleanup-users";
    private static final Duration LOCK_TTL = Duration.ofHours(1).plusMinutes(1);

    private final LockServiceBase lockService;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;
    private final Clock clock;

    @Scheduled(cron = "${scheduler.user-cleanup-cron:0 0 2 * * ?}")
    public void cleanupInactiveUsers() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            LocalDateTime cutoff = LocalDateTime.now(clock)
                    .minusYears(schedulerProperties.getUserInactiveYears());

            int cleaned = 0;

            // Clients
            var inactiveClients = clientRepository.findInactiveClientsBefore(cutoff);
            inactiveClients.forEach(client -> {
                client.setDeleted(true);
                client.setEmail("deleted-" + client.getId() + "@aroundu.local");
                client.setPhoneNumber("DEL" + client.getId());
            });
            clientRepository.saveAll(inactiveClients);
            cleaned += inactiveClients.size();

            // Workers
            var inactiveWorkers = workerRepository.findInactiveWorkersBefore(cutoff);
            inactiveWorkers.forEach(worker -> {
                worker.setDeleted(true);
                worker.setEmail("deleted-" + worker.getId() + "@aroundu.local");
                worker.setPhoneNumber("DEL" + worker.getId());
            });
            workerRepository.saveAll(inactiveWorkers);
            cleaned += inactiveWorkers.size();

            long durationMs = System.currentTimeMillis() - start;
            log.info("Cleaned {} inactive users ({}ms)", cleaned, durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("User cleanup failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }
}
