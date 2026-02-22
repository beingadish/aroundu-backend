package com.beingadish.AroundU.Scheduler;

import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.common.constants.enums.Country;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.infrastructure.analytics.entity.AggregatedMetrics;
import com.beingadish.AroundU.infrastructure.analytics.repository.AggregatedMetricsRepository;
import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.lock.NoOpLockService;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import com.beingadish.AroundU.infrastructure.scheduler.*;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.event.JobExpiredEvent;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.location.service.JobGeoService;
import com.beingadish.AroundU.notification.service.EmailService;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for all five schedulers:
 * <ul>
 * <li>UserCleanupScheduler</li>
 * <li>JobExpirationScheduler</li>
 * <li>ReminderScheduler</li>
 * <li>CacheSyncScheduler</li>
 * <li>AnalyticsScheduler</li>
 * </ul>
 * Uses {@link Clock#fixed} for deterministic time control and verifies lock
 * acquisition, database effects, event publishing, and metrics recording.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Scheduled Tasks")
class SchedulerTest {

    // ── Shared mocks ─────────────────────────────────────────────────────
    private SchedulerProperties props;
    private SchedulerMetricsService schedulerMetrics;

    private static Address testAddress() {
        return Address.builder()
                .country(Country.US)
                .postalCode("10001")
                .latitude(40.7128)
                .longitude(-74.006)
                .build();
    }

    /**
     * Set private {@code id} field via reflection for entities without setters.
     */
    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = findIdField(entity.getClass());
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id on " + entity.getClass().getSimpleName(), e);
        }
    }

    private static java.lang.reflect.Field findIdField(Class<?> clazz) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }

    @BeforeEach
    void baseSetUp() {
        props = new SchedulerProperties();
        props.setEnabled(true);
        schedulerMetrics = new SchedulerMetricsService(new SimpleMeterRegistry());
    }

    /**
     * Stub lock service that grants or denies all lock requests.
     */
    static class StubLockService extends LockServiceBase {

        final boolean grant;
        int acquireCount = 0;
        int releaseCount = 0;

        StubLockService(boolean grant) {
            this.grant = grant;
        }

        @Override
        public boolean tryAcquireLock(String taskName, Duration ttl) {
            acquireCount++;
            return grant;
        }

        @Override
        public void releaseLock(String taskName) {
            releaseCount++;
        }
    }

    // =====================================================================
    //  1 · LockService (no-op for tests)
    // =====================================================================
    @Nested
    @DisplayName("LockServiceBase")
    class LockServiceBaseTests {

        @Test
        @DisplayName("NoOpLockService always grants the lock")
        void noOpAlwaysGrants() {
            NoOpLockService noOp = new NoOpLockService();
            assertThat(noOp.tryAcquireLock("test-task", Duration.ofMinutes(5))).isTrue();
            // release should not throw
            assertThatCode(() -> noOp.releaseLock("test-task")).doesNotThrowAnyException();
        }
    }

    // =====================================================================
    //  2 · UserCleanupScheduler
    // =====================================================================
    @Nested
    @DisplayName("UserCleanupScheduler")
    class UserCleanupTests {

        private static final Instant FIXED_INSTANT
                = LocalDateTime.of(2026, 2, 18, 2, 0).toInstant(ZoneOffset.UTC);
        @Mock
        private ClientRepository clientRepository;
        @Mock
        private WorkerRepository workerRepository;
        private StubLockService lockService;
        private UserCleanupScheduler scheduler;

        @BeforeEach
        void setUp() {
            lockService = new StubLockService(true);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new UserCleanupScheduler(
                    lockService, clientRepository, workerRepository,
                    props, schedulerMetrics, fixedClock);
        }

        @Test
        @DisplayName("cleans up inactive clients and workers")
        void cleansInactiveUsers() {
            Client inactive = Client.builder()
                    .id(42L)
                    .name("Old User")
                    .email("old@test.com")
                    .phoneNumber("+1234567890")
                    .hashedPassword("hash")
                    .currentAddress(testAddress())
                    .deleted(false)
                    .build();
            Worker inactiveWorker = Worker.builder()
                    .id(77L)
                    .name("Old Worker")
                    .email("oldworker@test.com")
                    .phoneNumber("+0987654321")
                    .hashedPassword("hash")
                    .currentAddress(testAddress())
                    .deleted(false)
                    .build();

            when(clientRepository.findInactiveClientsBefore(any())).thenReturn(List.of(inactive));
            when(workerRepository.findInactiveWorkersBefore(any())).thenReturn(List.of(inactiveWorker));

            scheduler.cleanupInactiveUsers();

            // Verify soft-delete + anonymisation
            assertThat(inactive.getDeleted()).isTrue();
            assertThat(inactive.getEmail()).isEqualTo("deleted-42@aroundu.local");
            assertThat(inactive.getPhoneNumber()).isEqualTo("DEL42");

            assertThat(inactiveWorker.getDeleted()).isTrue();
            assertThat(inactiveWorker.getEmail()).isEqualTo("deleted-77@aroundu.local");
            assertThat(inactiveWorker.getPhoneNumber()).isEqualTo("DEL77");

            verify(clientRepository).saveAll(List.of(inactive));
            verify(workerRepository).saveAll(List.of(inactiveWorker));

            // Lock was acquired and released
            assertThat(lockService.acquireCount).isEqualTo(1);
            assertThat(lockService.releaseCount).isEqualTo(1);

            // Metrics recorded
            assertThat(schedulerMetrics.getLastExecutionTime("cleanup-users")).isNotNull();
        }

        @Test
        @DisplayName("skips when scheduler is disabled")
        void skipsWhenDisabled() {
            props.setEnabled(false);
            scheduler.cleanupInactiveUsers();
            verifyNoInteractions(clientRepository, workerRepository);
        }

        @Test
        @DisplayName("skips when lock cannot be acquired")
        void skipsWhenLocked() {
            lockService = new StubLockService(false);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new UserCleanupScheduler(
                    lockService, clientRepository, workerRepository,
                    props, schedulerMetrics, fixedClock);

            scheduler.cleanupInactiveUsers();
            verifyNoInteractions(clientRepository, workerRepository);
        }

        @Test
        @DisplayName("uses correct cutoff date based on configured years")
        void correctCutoff() {
            props.setUserInactiveYears(3);

            when(clientRepository.findInactiveClientsBefore(any())).thenReturn(List.of());
            when(workerRepository.findInactiveWorkersBefore(any())).thenReturn(List.of());

            scheduler.cleanupInactiveUsers();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(clientRepository).findInactiveClientsBefore(captor.capture());

            LocalDateTime expected = LocalDateTime.of(2023, 2, 18, 2, 0);
            assertThat(captor.getValue()).isEqualTo(expected);
        }

        @Test
        @DisplayName("handles empty result gracefully")
        void handlesEmptyResult() {
            when(clientRepository.findInactiveClientsBefore(any())).thenReturn(List.of());
            when(workerRepository.findInactiveWorkersBefore(any())).thenReturn(List.of());

            assertThatCode(() -> scheduler.cleanupInactiveUsers()).doesNotThrowAnyException();
            verify(clientRepository).saveAll(List.of());
            verify(workerRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("releases lock even on exception")
        void releasesLockOnException() {
            when(clientRepository.findInactiveClientsBefore(any()))
                    .thenThrow(new RuntimeException("DB error"));

            scheduler.cleanupInactiveUsers();
            assertThat(lockService.releaseCount).isEqualTo(1);
        }
    }

    // =====================================================================
    //  3 · JobExpirationScheduler
    // =====================================================================
    @Nested
    @DisplayName("JobExpirationScheduler")
    class JobExpirationTests {

        private static final Instant FIXED_INSTANT
                = LocalDateTime.of(2026, 2, 18, 12, 0).toInstant(ZoneOffset.UTC);
        @Mock
        private JobRepository jobRepository;
        @Mock
        private JobGeoService jobGeoService;
        @Mock
        private ApplicationEventPublisher eventPublisher;
        private StubLockService lockService;
        private JobExpirationScheduler scheduler;

        @BeforeEach
        void setUp() {
            lockService = new StubLockService(true);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new JobExpirationScheduler(
                    lockService, jobRepository, jobGeoService,
                    eventPublisher, props, schedulerMetrics, fixedClock);
        }

        @Test
        @DisplayName("closes expired jobs and removes from geo index")
        void closesExpiredJobs() {
            Client client = Client.builder()
                    .name("Test Client")
                    .email("client@test.com")
                    .phoneNumber("+1234567890")
                    .hashedPassword("hash")
                    .currentAddress(testAddress())
                    .build();
            // Reflection to set ID since @GeneratedValue
            setId(client, 1L);

            Job expiredJob = Job.builder()
                    .title("Expired Job")
                    .longDescription("desc")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(testAddress())
                    .createdBy(client)
                    .build();
            setId(expiredJob, 42L);

            when(jobRepository.findExpiredJobs(eq(JobStatus.OPEN_FOR_BIDS), any(), any()))
                    .thenReturn(List.of(expiredJob));

            scheduler.closeExpiredJobs();

            assertThat(expiredJob.getJobStatus())
                    .isEqualTo(JobStatus.JOB_CLOSED_DUE_TO_EXPIRATION);
            verify(jobRepository).save(expiredJob);
            verify(jobGeoService).removeOpenJob(42L);
            verify(eventPublisher).publishEvent(any(JobExpiredEvent.class));
        }

        @Test
        @DisplayName("handles no expired jobs gracefully")
        void noExpiredJobs() {
            when(jobRepository.findExpiredJobs(any(), any(), any())).thenReturn(List.of());
            scheduler.closeExpiredJobs();
            verify(jobRepository, never()).save(any());
            verify(jobGeoService, never()).removeOpenJob(any());
        }

        @Test
        @DisplayName("publishes JobExpiredEvent with correct data")
        void publishesCorrectEvent() {
            Client client = Client.builder()
                    .name("C")
                    .email("c@t.com")
                    .phoneNumber("+1")
                    .hashedPassword("h")
                    .currentAddress(testAddress())
                    .build();
            setId(client, 5L);

            Job job = Job.builder()
                    .title("J")
                    .longDescription("d")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(testAddress())
                    .createdBy(client)
                    .build();
            setId(job, 10L);

            when(jobRepository.findExpiredJobs(any(), any(), any())).thenReturn(List.of(job));

            scheduler.closeExpiredJobs();

            ArgumentCaptor<JobExpiredEvent> cap = ArgumentCaptor.forClass(JobExpiredEvent.class);
            verify(eventPublisher).publishEvent(cap.capture());
            assertThat(cap.getValue().jobId()).isEqualTo(10L);
            assertThat(cap.getValue().clientId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("skips when lock not acquired")
        void skipsWhenLocked() {
            lockService = new StubLockService(false);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new JobExpirationScheduler(
                    lockService, jobRepository, jobGeoService,
                    eventPublisher, props, schedulerMetrics, fixedClock);

            scheduler.closeExpiredJobs();
            verifyNoInteractions(jobRepository);
        }
    }

    // =====================================================================
    //  4 · ReminderScheduler
    // =====================================================================
    @Nested
    @DisplayName("ReminderScheduler")
    class ReminderTests {

        private static final Instant FIXED_INSTANT
                = LocalDateTime.of(2026, 2, 18, 6, 0).toInstant(ZoneOffset.UTC);
        @Mock
        private JobRepository jobRepository;
        @Mock
        private EmailService emailService;
        private StubLockService lockService;
        private ReminderScheduler scheduler;

        @BeforeEach
        void setUp() {
            lockService = new StubLockService(true);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new ReminderScheduler(
                    lockService, jobRepository, emailService,
                    props, schedulerMetrics, fixedClock);
        }

        @Test
        @DisplayName("sends reminder emails for zero-bid jobs")
        void sendsReminders() {
            Client client = Client.builder()
                    .name("Client")
                    .email("client@test.com")
                    .phoneNumber("+1234567890")
                    .hashedPassword("hash")
                    .currentAddress(testAddress())
                    .build();
            setId(client, 1L);

            Job job = Job.builder()
                    .title("My Job")
                    .longDescription("desc")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(testAddress())
                    .createdBy(client)
                    .build();
            setId(job, 7L);

            when(jobRepository.findJobsWithZeroBids(eq(JobStatus.OPEN_FOR_BIDS), any()))
                    .thenReturn(List.of(job));
            when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

            scheduler.sendBidReminders();

            verify(emailService).sendEmail(
                    eq("client@test.com"),
                    contains("My Job"),
                    contains("hasn't received any bids"));
        }

        @Test
        @DisplayName("skips deleted/anonymised email addresses")
        void skipsDeletedEmails() {
            Client deletedClient = Client.builder()
                    .name("Deleted")
                    .email("deleted@aroundu.local")
                    .phoneNumber("0000000000")
                    .hashedPassword("hash")
                    .currentAddress(testAddress())
                    .deleted(true)
                    .build();

            Job job = Job.builder()
                    .title("Orphan Job")
                    .longDescription("desc")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(testAddress())
                    .createdBy(deletedClient)
                    .build();

            when(jobRepository.findJobsWithZeroBids(any(), any())).thenReturn(List.of(job));

            scheduler.sendBidReminders();
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("uses configured threshold hours")
        void usesConfiguredThreshold() {
            props.setReminderThresholdHours(48);

            when(jobRepository.findJobsWithZeroBids(any(), any())).thenReturn(List.of());

            scheduler.sendBidReminders();

            ArgumentCaptor<LocalDateTime> cap = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(jobRepository).findJobsWithZeroBids(any(), cap.capture());

            LocalDateTime expected = LocalDateTime.of(2026, 2, 16, 6, 0);
            assertThat(cap.getValue()).isEqualTo(expected);
        }
    }

    // =====================================================================
    //  5 · CacheSyncScheduler
    // =====================================================================
    @Nested
    @DisplayName("CacheSyncScheduler")
    class CacheSyncTests {

        @Mock
        private JobRepository jobRepository;
        @Mock
        private JobGeoService jobGeoService;
        private StubLockService lockService;
        private CacheSyncScheduler scheduler;

        @BeforeEach
        void setUp() {
            lockService = new StubLockService(true);
            scheduler = new CacheSyncScheduler(
                    lockService, jobRepository, jobGeoService,
                    props, schedulerMetrics);
        }

        @Test
        @DisplayName("adds missing jobs and removes stale entries")
        void syncsCorrectly() {
            // Redis has jobs 1, 2, 3 (3 is stale)
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("1", "2", "3"));
            // Postgres says only 1, 2, 4 are open (4 is missing)
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS))
                    .thenReturn(List.of(1L, 2L, 4L));

            Job job4 = Job.builder()
                    .title("New Job")
                    .longDescription("desc")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(Address.builder()
                            .country(Country.US)
                            .postalCode("10001")
                            .latitude(40.7128)
                            .longitude(-74.006)
                            .build())
                    .build();
            setId(job4, 4L);
            when(jobRepository.findById(4L)).thenReturn(Optional.of(job4));

            scheduler.syncRedisWithPostgres();

            // Job 4 added to geo
            verify(jobGeoService).addOrUpdateOpenJob(4L, 40.7128, -74.006);
            // Job 3 removed from geo
            verify(jobGeoService).removeOpenJob(3L);
            // Jobs 1,2 not touched
            verify(jobGeoService, never()).addOrUpdateOpenJob(eq(1L), any(), any());
            verify(jobGeoService, never()).addOrUpdateOpenJob(eq(2L), any(), any());
            verify(jobGeoService, never()).removeOpenJob(1L);
            verify(jobGeoService, never()).removeOpenJob(2L);
        }

        @Test
        @DisplayName("handles empty Redis gracefully")
        void emptyRedis() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of());
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS))
                    .thenReturn(List.of(1L));
            Job job1 = Job.builder()
                    .title("J")
                    .longDescription("d")
                    .jobStatus(JobStatus.OPEN_FOR_BIDS)
                    .jobUrgency(JobUrgency.NORMAL)
                    .jobLocation(Address.builder()
                            .country(Country.US)
                            .postalCode("10001")
                            .latitude(1.0)
                            .longitude(2.0)
                            .build())
                    .build();
            setId(job1, 1L);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job1));

            scheduler.syncRedisWithPostgres();

            verify(jobGeoService).addOrUpdateOpenJob(1L, 1.0, 2.0);
        }

        @Test
        @DisplayName("does nothing when Redis and Postgres are in sync")
        void alreadyInSync() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("1", "2"));
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS))
                    .thenReturn(List.of(1L, 2L));

            scheduler.syncRedisWithPostgres();

            verify(jobGeoService, never()).addOrUpdateOpenJob(any(), any(), any());
            verify(jobGeoService, never()).removeOpenJob(any());
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    // =====================================================================
    //  6 · AnalyticsScheduler
    // =====================================================================
    @Nested
    @DisplayName("AnalyticsScheduler")
    class AnalyticsTests {

        private static final Instant FIXED_INSTANT
                = LocalDateTime.of(2026, 2, 18, 3, 0).toInstant(ZoneOffset.UTC);
        @Mock
        private JobRepository jobRepository;
        @Mock
        private BidRepository bidRepository;
        @Mock
        private PaymentTransactionRepository paymentRepository;
        @Mock
        private AggregatedMetricsRepository metricsRepository;
        private StubLockService lockService;
        private AnalyticsScheduler scheduler;

        @BeforeEach
        void setUp() {
            lockService = new StubLockService(true);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            scheduler = new AnalyticsScheduler(
                    lockService, jobRepository, bidRepository, paymentRepository,
                    metricsRepository, props, schedulerMetrics, fixedClock);
        }

        @Test
        @DisplayName("aggregates yesterday's metrics correctly")
        void aggregatesMetrics() {
            LocalDate yesterday = LocalDate.of(2026, 2, 17);

            when(metricsRepository.existsByMetricDate(yesterday)).thenReturn(false);
            when(jobRepository.countByCreatedAtBetween(any(), any())).thenReturn(10L);
            when(bidRepository.countByCreatedAtBetween(any(), any())).thenReturn(25L);
            when(jobRepository.countByJobStatusAndCreatedAtBetween(eq(JobStatus.COMPLETED), any(), any()))
                    .thenReturn(3L);
            when(paymentRepository.sumAmountByStatusAndCreatedAtBetween(
                    eq(PaymentStatus.RELEASED), any(), any())).thenReturn(1500.0);
            when(metricsRepository.findByMetricDate(any())).thenReturn(Optional.empty());

            scheduler.aggregateDailyMetrics();

            ArgumentCaptor<AggregatedMetrics> cap = ArgumentCaptor.forClass(AggregatedMetrics.class);
            verify(metricsRepository).save(cap.capture());

            AggregatedMetrics saved = cap.getValue();
            assertThat(saved.getMetricDate()).isEqualTo(yesterday);
            assertThat(saved.getJobsCreated()).isEqualTo(10L);
            assertThat(saved.getBidsPlaced()).isEqualTo(25L);
            assertThat(saved.getJobsCompleted()).isEqualTo(3L);
            assertThat(saved.getRevenueTotal()).isEqualTo(1500.0);
            assertThat(saved.getAverageBidPerJob()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("skips aggregation if already computed for yesterday")
        void skipsIfAlreadyComputed() {
            when(metricsRepository.existsByMetricDate(LocalDate.of(2026, 2, 17)))
                    .thenReturn(true);

            scheduler.aggregateDailyMetrics();

            verify(metricsRepository, never()).save(any());
        }

        @Test
        @DisplayName("computes week-over-week growth")
        void computesWeekOverWeek() {
            LocalDate yesterday = LocalDate.of(2026, 2, 17);
            LocalDate lastWeek = yesterday.minusWeeks(1);

            when(metricsRepository.existsByMetricDate(yesterday)).thenReturn(false);
            when(jobRepository.countByCreatedAtBetween(any(), any())).thenReturn(20L);
            when(bidRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
            when(jobRepository.countByJobStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
            when(paymentRepository.sumAmountByStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(0.0);

            AggregatedMetrics lastWeekMetrics = AggregatedMetrics.builder()
                    .metricDate(lastWeek)
                    .jobsCreated(10L)
                    .build();
            when(metricsRepository.findByMetricDate(lastWeek))
                    .thenReturn(Optional.of(lastWeekMetrics));
            when(metricsRepository.findByMetricDate(yesterday.minusMonths(1)))
                    .thenReturn(Optional.empty());

            scheduler.aggregateDailyMetrics();

            ArgumentCaptor<AggregatedMetrics> cap = ArgumentCaptor.forClass(AggregatedMetrics.class);
            verify(metricsRepository).save(cap.capture());

            // 20 vs 10 = 100% growth
            assertThat(cap.getValue().getWeekOverWeekGrowth()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("handles zero jobsCreated gracefully for average")
        void zeroJobsAverage() {
            when(metricsRepository.existsByMetricDate(any())).thenReturn(false);
            when(jobRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
            when(bidRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);
            when(jobRepository.countByJobStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
            when(paymentRepository.sumAmountByStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(0.0);
            when(metricsRepository.findByMetricDate(any())).thenReturn(Optional.empty());

            scheduler.aggregateDailyMetrics();

            ArgumentCaptor<AggregatedMetrics> cap = ArgumentCaptor.forClass(AggregatedMetrics.class);
            verify(metricsRepository).save(cap.capture());
            assertThat(cap.getValue().getAverageBidPerJob()).isEqualTo(0.0);
        }
    }

    // =====================================================================
    //  7 · SchedulerMetricsService
    // =====================================================================
    @Nested
    @DisplayName("SchedulerMetricsService")
    class SchedulerMetricsTests {

        private SchedulerMetricsService service;

        @BeforeEach
        void setUp() {
            service = new SchedulerMetricsService(new SimpleMeterRegistry());
        }

        @Test
        @DisplayName("records success with timing data")
        void recordSuccess() {
            service.recordSuccess("test-task", 150L);

            assertThat(service.getLastExecutionTime("test-task")).isNotNull();
            assertThat(service.getLastDurationMs("test-task")).isEqualTo(150L);
        }

        @Test
        @DisplayName("records failure without updating last execution time")
        void recordFailure() {
            service.recordFailure("fail-task", 50L);

            assertThat(service.getLastExecutionTime("fail-task")).isNull();
            assertThat(service.getLastDurationMs("fail-task")).isNull();
        }

        @Test
        @DisplayName("returns null for unknown tasks")
        void unknownTask() {
            assertThat(service.getLastExecutionTime("nobody")).isNull();
            assertThat(service.getLastDurationMs("nobody")).isNull();
        }

        @Test
        @DisplayName("overwrites previous metrics on subsequent runs")
        void overwritesPrevious() {
            service.recordSuccess("task", 100L);
            Instant first = service.getLastExecutionTime("task");

            service.recordSuccess("task", 200L);
            Instant second = service.getLastExecutionTime("task");

            assertThat(second).isAfterOrEqualTo(first);
            assertThat(service.getLastDurationMs("task")).isEqualTo(200L);
        }
    }

    // =====================================================================
    //  8 · Cross-cutting: disabled scheduler
    // =====================================================================
    @Nested
    @DisplayName("Disabled scheduler master switch")
    class DisabledSchedulerTests {

        @Mock
        private JobRepository jobRepository;
        @Mock
        private JobGeoService jobGeoService;
        @Mock
        private ApplicationEventPublisher eventPublisher;

        @Test
        @DisplayName("JobExpirationScheduler does nothing when disabled")
        void jobExpDisabled() {
            props.setEnabled(false);
            StubLockService lock = new StubLockService(true);
            Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
            var scheduler = new JobExpirationScheduler(
                    lock, jobRepository, jobGeoService,
                    eventPublisher, props, schedulerMetrics, fixedClock);

            scheduler.closeExpiredJobs();
            verifyNoInteractions(jobRepository);
            assertThat(lock.acquireCount).isEqualTo(0);
        }
    }

    // =====================================================================
    //  9 · Time mocking verification
    // =====================================================================
    @Nested
    @DisplayName("Clock-based time control")
    class TimeControlTests {

        @Mock
        private ClientRepository clientRepository;
        @Mock
        private WorkerRepository workerRepository;

        @Test
        @DisplayName("advancing Clock.fixed changes the cutoff date")
        void advancingClockChangesCutoff() {
            StubLockService lock = new StubLockService(true);

            // First run at 2026-02-18
            Instant t1 = LocalDateTime.of(2026, 2, 18, 2, 0).toInstant(ZoneOffset.UTC);
            var sched1 = new UserCleanupScheduler(
                    lock, clientRepository, workerRepository,
                    props, schedulerMetrics, Clock.fixed(t1, ZoneOffset.UTC));

            when(clientRepository.findInactiveClientsBefore(any())).thenReturn(List.of());
            when(workerRepository.findInactiveWorkersBefore(any())).thenReturn(List.of());

            sched1.cleanupInactiveUsers();

            ArgumentCaptor<LocalDateTime> cap1 = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(clientRepository).findInactiveClientsBefore(cap1.capture());
            assertThat(cap1.getValue()).isEqualTo(LocalDateTime.of(2024, 2, 18, 2, 0));
            reset(clientRepository, workerRepository);

            // Second run "3 months later"
            Instant t2 = LocalDateTime.of(2026, 5, 18, 2, 0).toInstant(ZoneOffset.UTC);
            var sched2 = new UserCleanupScheduler(
                    lock, clientRepository, workerRepository,
                    props, schedulerMetrics, Clock.fixed(t2, ZoneOffset.UTC));

            when(clientRepository.findInactiveClientsBefore(any())).thenReturn(List.of());
            when(workerRepository.findInactiveWorkersBefore(any())).thenReturn(List.of());

            sched2.cleanupInactiveUsers();

            ArgumentCaptor<LocalDateTime> cap2 = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(clientRepository).findInactiveClientsBefore(cap2.capture());
            assertThat(cap2.getValue()).isEqualTo(LocalDateTime.of(2024, 5, 18, 2, 0));
        }
    }
}
