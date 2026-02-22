package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.user.service.WorkerPenaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every hour to unblock workers whose cancellation-penalty period has
 * expired.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class WorkerPenaltyScheduler {

    private final WorkerPenaltyService workerPenaltyService;

    @Scheduled(cron = "${worker.penalty.unblock-cron:0 0 * * * *}")
    public void unblockExpiredPenalties() {
        log.debug("Running worker penalty unblock check…");
        workerPenaltyService.unblockExpiredWorkers();
    }
}
