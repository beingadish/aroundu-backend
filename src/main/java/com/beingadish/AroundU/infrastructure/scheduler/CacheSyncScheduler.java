package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.location.service.JobGeoService;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Periodically reconciles the Redis geo-index with the PostgreSQL source of
 * truth. Adds any OPEN_FOR_BIDS jobs missing from Redis and removes stale
 * entries that no longer qualify.
 * <p>
 * Default schedule: every 30 minutes.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CacheSyncScheduler {

    private static final String TASK_NAME = "cache-sync";
    private static final Duration LOCK_TTL = Duration.ofMinutes(15).plusMinutes(1);

    private final LockServiceBase lockService;
    private final JobRepository jobRepository;
    private final JobGeoService jobGeoService;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;

    @Scheduled(cron = "${scheduler.cache-sync-cron:0 */30 * * * ?}")
    public void syncRedisWithPostgres() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            // 1. Current state in Redis
            Set<String> geoMembers = jobGeoService.getAllGeoMembers();
            Set<Long> redisJobIds = geoMembers.stream()
                    .map(s -> {
                        try {
                            return Long.valueOf(s);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            // 2. Source of truth in PostgreSQL
            List<Long> openIds = jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS);
            Set<Long> pgJobIds = Set.copyOf(openIds);

            // 3. Missing in Redis → add them
            int added = 0;
            for (Long pgId : pgJobIds) {
                if (!redisJobIds.contains(pgId)) {
                    Optional<Job> jobOpt = jobRepository.findById(pgId);
                    if (jobOpt.isPresent()) {
                        Job job = jobOpt.get();
                        Address loc = job.getJobLocation();
                        if (loc != null && loc.getLatitude() != null && loc.getLongitude() != null) {
                            jobGeoService.addOrUpdateOpenJob(job.getId(),
                                    loc.getLatitude(), loc.getLongitude());
                            added++;
                        }
                    }
                }
            }

            // 4. Stale in Redis → remove them
            int removed = 0;
            for (Long redisId : redisJobIds) {
                if (!pgJobIds.contains(redisId)) {
                    jobGeoService.removeOpenJob(redisId);
                    removed++;
                }
            }

            long durationMs = System.currentTimeMillis() - start;
            log.info("Cache sync: added {} missing, removed {} stale ({}ms)",
                    added, removed, durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("Cache sync failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }
}
