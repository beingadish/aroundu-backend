package com.beingadish.AroundU.infrastructure.cache;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.common.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import com.beingadish.AroundU.job.service.JobService;

/**
 * Pre-loads frequently accessed data into the Redis cache on application
 * startup to minimise cold-start latency.
 * <p>
 * Runs after the geo-sync ({@link JobGeoSyncService}) completes.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final JobRepository jobRepository;
    private final JobService jobService;
    private final SkillRepository skillRepository;

    /**
     * Pre-loads:
     * <ol>
     * <li>Top 100 recent open jobs into the {@code job:detail} cache</li>
     * <li>All skills (lightweight, rarely changes)</li>
     * </ol>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void warmCaches() {
        long start = System.currentTimeMillis();
        log.info("Cache warmup starting...");

        // 1. Pre-load top 100 recent open jobs
        List<Job> recentJobs = jobRepository
                .findTop100ByJobStatusOrderByCreatedAtDesc(JobStatus.OPEN_FOR_BIDS);
        int jobsWarmed = 0;
        for (Job job : recentJobs) {
            try {
                jobService.getJobDetail(job.getId()); // triggers @Cacheable → fills cache
                jobsWarmed++;
            } catch (Exception ex) {
                log.debug("Skipped cache warmup for jobId={}: {}", job.getId(), ex.getMessage());
            }
        }

        // 2. Pre-load all skills (count only — caching of skill lookups is a future enhancement)
        long skillCount = skillRepository.count();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Cache warmup completed in {}ms: {} jobs cached, {} skills counted",
                elapsed, jobsWarmed, skillCount);
    }
}
