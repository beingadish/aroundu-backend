package com.beingadish.AroundU.Scheduler;

import com.beingadish.AroundU.Config.SchedulerProperties;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Events.JobExpiredEvent;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Service.JobGeoService;
import com.beingadish.AroundU.Service.LockServiceBase;
import com.beingadish.AroundU.Service.SchedulerMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Closes jobs that have passed their scheduled start time or exceeded the
 * configurable maximum open duration. Removes expired jobs from the Redis
 * geo-index and publishes a {@link JobExpiredEvent} for downstream notification
 * handling.
 * <p>
 * Default schedule: every hour.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class JobExpirationScheduler {

    private static final String TASK_NAME = "expire-jobs";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30).plusMinutes(1);

    private final LockServiceBase lockService;
    private final JobRepository jobRepository;
    private final JobGeoService jobGeoService;
    private final ApplicationEventPublisher eventPublisher;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;
    private final Clock clock;

    @Scheduled(cron = "${scheduler.job-expiration-cron:0 0 * * * ?}")
    public void closeExpiredJobs() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime fallbackCutoff = now.minusDays(schedulerProperties.getJobExpirationDays());

            List<Job> expiredJobs = jobRepository.findExpiredJobs(
                    JobStatus.OPEN_FOR_BIDS, now, fallbackCutoff);

            for (Job job : expiredJobs) {
                job.setJobStatus(JobStatus.JOB_CLOSED_DUE_TO_EXPIRATION);
                jobRepository.save(job);
                jobGeoService.removeOpenJob(job.getId());
                eventPublisher.publishEvent(
                        new JobExpiredEvent(job.getId(), job.getCreatedBy().getId()));
            }

            long durationMs = System.currentTimeMillis() - start;
            log.info("Closed {} expired jobs ({}ms)", expiredJobs.size(), durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("Job expiration failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }
}
