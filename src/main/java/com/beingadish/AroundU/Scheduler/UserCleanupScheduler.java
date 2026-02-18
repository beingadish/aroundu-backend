package com.beingadish.AroundU.Scheduler;

import com.beingadish.AroundU.Config.SchedulerProperties;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Service.LockServiceBase;
import com.beingadish.AroundU.Service.SchedulerMetricsService;
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
                client.setEmail("deleted@aroundu.local");
                client.setPhoneNumber("0000000000");
            });
            clientRepository.saveAll(inactiveClients);
            cleaned += inactiveClients.size();

            // Workers
            var inactiveWorkers = workerRepository.findInactiveWorkersBefore(cutoff);
            inactiveWorkers.forEach(worker -> {
                worker.setDeleted(true);
                worker.setEmail("deleted@aroundu.local");
                worker.setPhoneNumber("0000000000");
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
