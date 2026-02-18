package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Events.JobModifiedEvent;
import com.beingadish.AroundU.Repository.Job.JobRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the Redis geo-index and Spring caches consistent with the PostgreSQL
 * source of truth.
 * <ul>
 * <li>On startup → bulk-sync all OPEN_FOR_BIDS jobs into the geo-index</li>
 * <li>Daily at 02:00 → prune stale entries from the geo-index</li>
 * <li>After each job mutation commits → granular cache eviction</li>
 * </ul>
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class JobGeoSyncService {

    private final JobRepository jobRepository;
    private final JobGeoService jobGeoService;
    private final CacheEvictionService cacheEvictionService;

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

        // Worker feed eviction for structural changes only
        if (event.type() != JobModifiedEvent.Type.UPDATED) {
            cacheEvictionService.evictWorkerFeedCaches();
        }
    }
}
