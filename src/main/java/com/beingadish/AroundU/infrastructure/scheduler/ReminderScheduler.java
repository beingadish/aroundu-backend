package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends reminder emails to clients whose open jobs have received zero bids
 * after a configurable threshold (default 24 hours). Suggests actions like
 * increasing budget or improving the description.
 * <p>
 * Default schedule: every 6 hours.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private static final String TASK_NAME = "bid-reminders";
    private static final Duration LOCK_TTL = Duration.ofHours(1).plusMinutes(1);

    private final LockServiceBase lockService;
    private final JobRepository jobRepository;
    private final EmailService emailService;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;
    private final Clock clock;

    @Scheduled(cron = "${scheduler.reminder-cron:0 0 */6 * * ?}")
    public void sendBidReminders() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            LocalDateTime threshold = LocalDateTime.now(clock)
                    .minusHours(schedulerProperties.getReminderThresholdHours());

            List<Job> zeroBidJobs = jobRepository.findJobsWithZeroBids(
                    JobStatus.OPEN_FOR_BIDS, threshold);

            int sent = 0;
            for (Job job : zeroBidJobs) {
                String clientEmail = job.getCreatedBy().getEmail();
                if (clientEmail == null || clientEmail.contains("deleted@")) {
                    continue;
                }
                String subject = "Your job \"" + job.getTitle() + "\" hasn't attracted bids yet";
                String body = buildReminderBody(job);
                if (emailService.sendEmail(clientEmail, subject, body)) {
                    sent++;
                }
            }

            long durationMs = System.currentTimeMillis() - start;
            log.info("Sent {} bid reminder emails for {} zero-bid jobs ({}ms)",
                    sent, zeroBidJobs.size(), durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("Reminder scheduler failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }

    private String buildReminderBody(Job job) {
        return """
                Hi %s,
                
                Your job "%s" was posted over %d hours ago but hasn't received any bids yet.
                
                Here are some tips to attract workers:
                • Increase the budget to match market rates
                • Add more detail to the job description
                • Ensure the job location is accurate
                • Add required skills to help workers find it
                
                View your job: https://aroundu.com/jobs/%d
                
                — The AroundU Team
                """.formatted(
                job.getCreatedBy().getName(),
                job.getTitle(),
                schedulerProperties.getReminderThresholdHours(),
                job.getId());
    }
}
