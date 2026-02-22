package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.infrastructure.cache.CacheEvictionService;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.event.JobModifiedEvent;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.location.repository.FailedGeoSyncRepository;
import com.beingadish.AroundU.location.service.JobGeoService;
import com.beingadish.AroundU.location.service.JobGeoSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobGeoSyncService")
class JobGeoSyncServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobGeoService jobGeoService;
    @Mock
    private CacheEvictionService cacheEvictionService;
    @Mock
    private FailedGeoSyncRepository failedGeoSyncRepository;

    @InjectMocks
    private JobGeoSyncService geoSyncService;

    // ── Startup Sync ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("Startup Sync")
    class StartupSyncTests {

        @Test
        @DisplayName("syncs all OPEN_FOR_BIDS jobs with valid coordinates")
        void syncsAllOpenJobs() {
            Address addr = Address.builder().latitude(40.7128).longitude(-74.0060).build();
            Job job1 = Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(addr).build();
            Job job2 = Job.builder().id(2L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(addr).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(job1, job2));

            geoSyncService.syncOpenJobsToGeoIndex();

            verify(jobGeoService).addOrUpdateOpenJob(1L, 40.7128, -74.0060);
            verify(jobGeoService).addOrUpdateOpenJob(2L, 40.7128, -74.0060);
        }

        @Test
        @DisplayName("skips jobs without location")
        void skipsJobsWithoutLocation() {
            Job noLocation = Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(null).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(noLocation));

            geoSyncService.syncOpenJobsToGeoIndex();

            verify(jobGeoService, never()).addOrUpdateOpenJob(anyLong(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("skips jobs with null coordinates")
        void skipsJobsWithNullCoordinates() {
            Address noCoords = Address.builder().latitude(null).longitude(null).build();
            Job job = Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(noCoords).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(job));

            geoSyncService.syncOpenJobsToGeoIndex();

            verify(jobGeoService, never()).addOrUpdateOpenJob(anyLong(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("handles empty job list gracefully")
        void handlesEmptyList() {
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(Collections.emptyList());

            geoSyncService.syncOpenJobsToGeoIndex();

            verify(jobGeoService, never()).addOrUpdateOpenJob(anyLong(), anyDouble(), anyDouble());
        }
    }

    // ── Daily Cleanup ────────────────────────────────────────────────────
    @Nested
    @DisplayName("Daily Cleanup")
    class DailyCleanupTests {

        @Test
        @DisplayName("removes stale entries not present in PostgreSQL")
        void removesStaleEntries() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("1", "2", "3"));
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(1L, 3L));

            geoSyncService.cleanupStaleGeoEntries();

            verify(jobGeoService).removeOpenJob(2L);
            verify(jobGeoService, never()).removeOpenJob(1L);
            verify(jobGeoService, never()).removeOpenJob(3L);
        }

        @Test
        @DisplayName("does nothing when geo index is empty")
        void emptyGeoIndex() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Collections.emptySet());

            geoSyncService.cleanupStaleGeoEntries();

            verify(jobRepository, never()).findIdsByJobStatus(any());
            verify(jobGeoService, never()).removeOpenJob(anyLong());
        }

        @Test
        @DisplayName("handles non-numeric geo member gracefully")
        void nonNumericMember() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("abc", "1"));
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(1L));

            geoSyncService.cleanupStaleGeoEntries();

            // "abc" triggers NumberFormatException → skipped; "1" is still active → not removed
            verify(jobGeoService, never()).removeOpenJob(1L);
        }

        @Test
        @DisplayName("removes all geo entries when PostgreSQL has none open")
        void removesAllWhenNoneOpen() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("10", "20"));
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(Collections.emptyList());

            geoSyncService.cleanupStaleGeoEntries();

            verify(jobGeoService).removeOpenJob(10L);
            verify(jobGeoService).removeOpenJob(20L);
        }
    }

    // ── Event-Driven Cache Eviction ──────────────────────────────────────
    @Nested
    @DisplayName("Event-Driven Cache Eviction")
    class EventCacheEvictionTests {

        @Test
        @DisplayName("CREATED event evicts job detail, client jobs, and worker feed")
        void createdEvent() {
            JobModifiedEvent event = new JobModifiedEvent(10L, 1L, JobModifiedEvent.Type.CREATED, false);

            geoSyncService.onJobModified(event);

            verify(cacheEvictionService).evictJobDetail(10L);
            verify(cacheEvictionService).evictClientJobsCaches(1L);
            verify(cacheEvictionService).evictWorkerFeedCaches();
        }

        @Test
        @DisplayName("UPDATED event evicts job detail and client jobs but NOT worker feed")
        void updatedEvent() {
            JobModifiedEvent event = new JobModifiedEvent(10L, 1L, JobModifiedEvent.Type.UPDATED, false);

            geoSyncService.onJobModified(event);

            verify(cacheEvictionService).evictJobDetail(10L);
            verify(cacheEvictionService).evictClientJobsCaches(1L);
            verify(cacheEvictionService, never()).evictWorkerFeedCaches();
        }

        @Test
        @DisplayName("STATUS_CHANGED event evicts all caches")
        void statusChangedEvent() {
            JobModifiedEvent event = new JobModifiedEvent(10L, 1L, JobModifiedEvent.Type.STATUS_CHANGED, false);

            geoSyncService.onJobModified(event);

            verify(cacheEvictionService).evictJobDetail(10L);
            verify(cacheEvictionService).evictClientJobsCaches(1L);
            verify(cacheEvictionService).evictWorkerFeedCaches();
        }

        @Test
        @DisplayName("DELETED event evicts all caches")
        void deletedEvent() {
            JobModifiedEvent event = new JobModifiedEvent(10L, 1L, JobModifiedEvent.Type.DELETED, false);

            geoSyncService.onJobModified(event);

            verify(cacheEvictionService).evictJobDetail(10L);
            verify(cacheEvictionService).evictClientJobsCaches(1L);
            verify(cacheEvictionService).evictWorkerFeedCaches();
        }

        @Test
        @DisplayName("events for different jobs are processed independently")
        void independentEvents() {
            geoSyncService.onJobModified(new JobModifiedEvent(1L, 10L, JobModifiedEvent.Type.UPDATED, false));
            geoSyncService.onJobModified(new JobModifiedEvent(2L, 20L, JobModifiedEvent.Type.STATUS_CHANGED, false));

            verify(cacheEvictionService).evictJobDetail(1L);
            verify(cacheEvictionService).evictJobDetail(2L);
            verify(cacheEvictionService).evictClientJobsCaches(10L);
            verify(cacheEvictionService).evictClientJobsCaches(20L);
        }
    }
}
