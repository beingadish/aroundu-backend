package com.beingadish.AroundU.location.service;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.infrastructure.cache.CacheEvictionService;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.event.JobModifiedEvent;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.location.entity.FailedGeoSync;
import com.beingadish.AroundU.location.repository.FailedGeoSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the Redis geo-index and Spring caches consistent with the PostgreSQL
 * source of truth.
 * <ul>
 * <li>On startup → bulk-sync all OPEN_FOR_BIDS jobs into the geo-index</li>
 * <li>Daily at 02:00 → prune stale entries from the geo-index</li>
 * <li>Every 5 minutes → retry failed geo-sync operations</li>
 * <li>After each job mutation commits → granular cache eviction</li>
 * </ul>
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class JobGeoSyncService {

    private static final int MAX_RETRIES = 5;

    private final JobRepository jobRepository;
    private final JobGeoService jobGeoService;
    private final CacheEvictionService cacheEvictionService;
    private final FailedGeoSyncRepository failedGeoSyncRepository;

    // ── Startup sync ─────────────────────────────────────────────────────

    /**
     * On application startup, sync all OPEN_FOR_BIDS jobs to the Redis geo
     * index so that proximity queries return results immediately.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public void syncOpenJobsToGeoIndex() {
        log.info("Starting geo-index sync for OPEN_FOR_BIDS jobs...");
        long start = System.currentTimeMillis();

        List<Job> openJobs = jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS);
        int synced = 0;
        for (Job job : openJobs) {
            if (job.getJobLocation() != null
                    && job.getJobLocation().getLatitude() != null
                    && job.getJobLocation().getLongitude() != null) {
                jobGeoService.addOrUpdateOpenJob(
                        job.getId(),
                        job.getJobLocation().getLatitude(),
                        job.getJobLocation().getLongitude());
                synced++;
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Synced {} jobs to Redis geo index in {}ms", synced, elapsed);
    }

    // ── Daily cleanup ────────────────────────────────────────────────────

    /**
     * Every day at 02:00 AM, remove geo-index entries whose jobs are no longer
     * OPEN_FOR_BIDS in PostgreSQL.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupStaleGeoEntries() {
        log.info("Starting daily geo-index cleanup...");
        long start = System.currentTimeMillis();

        Set<String> geoMembers = jobGeoService.getAllGeoMembers();
        if (geoMembers == null || geoMembers.isEmpty()) {
            log.info("Geo index is empty, nothing to clean up");
            return;
        }

        // IDs of jobs that should be in the geo-index
        List<Long> openIds = jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS);
        Set<String> openIdStrings = openIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        int removed = 0;
        for (String member : geoMembers) {
            if (!openIdStrings.contains(member)) {
                try {
                    jobGeoService.removeOpenJob(Long.valueOf(member));
                    removed++;
                } catch (NumberFormatException ex) {
                    log.warn("Non-numeric geo member skipped: {}", member);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Removed {} stale geo entries in {}ms", removed, elapsed);
    }

    // ── Retry failed geo-sync operations ─────────────────────────────────

    /**
     * Every 5 minutes, retry pending {@link FailedGeoSync} records that haven't
     * exceeded the maximum retry count.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryFailedGeoSyncs() {
        List<FailedGeoSync> pending = failedGeoSyncRepository
                .findByResolvedFalseAndRetryCountLessThanOrderByCreatedAtAsc(MAX_RETRIES);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed geo-sync operations...", pending.size());
        int succeeded = 0;
        int failed = 0;

        for (FailedGeoSync record : pending) {
            try {
                switch (record.getOperation()) {
                    case ADD, UPDATE -> {
                        // Verify the job still exists and is OPEN_FOR_BIDS
                        Optional<Job> jobOpt = jobRepository.findById(record.getJobId());
                        if (jobOpt.isPresent() && jobOpt.get().getJobStatus() == JobStatus.OPEN_FOR_BIDS) {
                            Job job = jobOpt.get();
                            Address loc = job.getJobLocation();
                            Double lat = record.getLatitude() != null ? record.getLatitude()
                                    : (loc != null ? loc.getLatitude() : null);
                            Double lon = record.getLongitude() != null ? record.getLongitude()
                                    : (loc != null ? loc.getLongitude() : null);
                            jobGeoService.addOrUpdateOpenJob(record.getJobId(), lat, lon);
                        }
                        // Mark resolved even if job no longer exists/is open
                        record.setResolved(true);
                        succeeded++;
                    }
                    case REMOVE -> {
                        jobGeoService.removeOpenJob(record.getJobId());
                        record.setResolved(true);
                        succeeded++;
                    }
                }
            } catch (Exception ex) {
                record.setRetryCount(record.getRetryCount() + 1);
                record.setLastError(ex.getMessage());
                failed++;
                log.warn("Retry {} failed for jobId={} op={}: {}",
                        record.getRetryCount(), record.getJobId(), record.getOperation(), ex.getMessage());
            }
            failedGeoSyncRepository.save(record);
        }

        log.info("Geo-sync retry complete: {} succeeded, {} failed", succeeded, failed);
    }

    // ── Event-driven cache eviction (runs AFTER transaction commits) ─────

    /**
     * Granular cache eviction triggered after a job mutation commits.
     * <ul>
     * <li>Job detail cache → evict the specific job ID</li>
     * <li>Client jobs list → evict only the affected client's keys</li>
     * <li>Worker feed → evict for structural changes (create, status,
     * delete)</li>
     * </ul>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobModified(JobModifiedEvent event) {
        log.debug("Processing JobModifiedEvent: job={} client={} type={}",
                event.jobId(), event.clientId(), event.type());

        // Granular job-detail eviction (single key)
        cacheEvictionService.evictJobDetail(event.jobId());

        // Evict only the affected client's list caches (pattern scan)
        cacheEvictionService.evictClientJobsCaches(event.clientId());

        // Worker feed eviction for structural changes or location updates
        if (event.type() != JobModifiedEvent.Type.UPDATED || event.locationChanged()) {
            cacheEvictionService.evictWorkerFeedCaches();
        }
    }
}
