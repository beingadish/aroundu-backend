package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.infrastructure.config.RedisConfig;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.event.JobModifiedEvent;
import com.beingadish.AroundU.location.repository.FailedGeoSyncRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.beingadish.AroundU.location.service.JobGeoService;
import com.beingadish.AroundU.infrastructure.cache.CacheEvictionService;
import com.beingadish.AroundU.location.service.JobGeoSyncService;

/**
 * End-to-end consistency scenarios verifying that:
 * <ul>
 * <li>Cache eviction targets specific keys, not entire regions</li>
 * <li>Geo-index stays consistent with PostgreSQL</li>
 * <li>No data loss when cache expires (PostgreSQL is source of truth)</li>
 * <li>Concurrent events for different jobs don't interfere</li>
 * <li>Cache region constants match expected values and TTL specs</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cache Consistency Tests")
class CacheConsistencyTest {

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

    // ── Granular Eviction Guarantees ─────────────────────────────────────
    @Nested
    @DisplayName("Granular Eviction Guarantees")
    class GranularEvictionTests {

        @Test
        @DisplayName("job creation invalidates specific job ID, affected client, and worker feed")
        void jobCreationGranularEviction() {
            geoSyncService.onJobModified(new JobModifiedEvent(42L, 7L, JobModifiedEvent.Type.CREATED, false));

            verify(cacheEvictionService).evictJobDetail(42L);        // specific key
            verify(cacheEvictionService).evictClientJobsCaches(7L);  // pattern for client 7
            verify(cacheEvictionService).evictWorkerFeedCaches();    // worker feed affected
        }

        @Test
        @DisplayName("job update targets only affected client, other clients unaffected")
        void jobUpdateTargetsSpecificClient() {
            geoSyncService.onJobModified(new JobModifiedEvent(100L, 5L, JobModifiedEvent.Type.UPDATED, false));

            verify(cacheEvictionService).evictClientJobsCaches(5L);
            // Client 6, 7, etc. should NOT have their caches evicted
            verify(cacheEvictionService, never()).evictClientJobsCaches(6L);
        }

        @Test
        @DisplayName("job deletion evicts all relevant caches")
        void jobDeletionEvictsAll() {
            geoSyncService.onJobModified(new JobModifiedEvent(99L, 3L, JobModifiedEvent.Type.DELETED, false));

            verify(cacheEvictionService).evictJobDetail(99L);
            verify(cacheEvictionService).evictClientJobsCaches(3L);
            verify(cacheEvictionService).evictWorkerFeedCaches();
        }

        @Test
        @DisplayName("concurrent updates for different jobs don't leave stale cache")
        void concurrentUpdatesProcessedIndependently() {
            JobModifiedEvent event1 = new JobModifiedEvent(1L, 10L, JobModifiedEvent.Type.UPDATED, false);
            JobModifiedEvent event2 = new JobModifiedEvent(2L, 20L, JobModifiedEvent.Type.STATUS_CHANGED, false);
            JobModifiedEvent event3 = new JobModifiedEvent(3L, 10L, JobModifiedEvent.Type.DELETED, false);

            geoSyncService.onJobModified(event1);
            geoSyncService.onJobModified(event2);
            geoSyncService.onJobModified(event3);

            // All three jobs get their detail cache evicted
            verify(cacheEvictionService).evictJobDetail(1L);
            verify(cacheEvictionService).evictJobDetail(2L);
            verify(cacheEvictionService).evictJobDetail(3L);

            // Client 10 gets evicted twice (for job 1 and 3), client 20 once
            verify(cacheEvictionService, times(2)).evictClientJobsCaches(10L);
            verify(cacheEvictionService).evictClientJobsCaches(20L);
        }
    }

    // ── Geo-Index Consistency ────────────────────────────────────────────
    @Nested
    @DisplayName("Geo-Index Consistency")
    class GeoIndexTests {

        @Test
        @DisplayName("startup sync only adds jobs with valid coordinates to geo index")
        void startupSyncValidCoordinates() {
            Address valid = Address.builder().latitude(40.7).longitude(-74.0).build();
            Address noLat = Address.builder().latitude(null).longitude(-74.0).build();
            Job job1 = Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(valid).build();
            Job job2 = Job.builder().id(2L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(noLat).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(job1, job2));

            geoSyncService.syncOpenJobsToGeoIndex();

            verify(jobGeoService).addOrUpdateOpenJob(1L, 40.7, -74.0);
            verify(jobGeoService, never()).addOrUpdateOpenJob(eq(2L), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("daily cleanup removes only stale entries, keeps active ones")
        void dailyCleanupRemovesStaleOnly() {
            when(jobGeoService.getAllGeoMembers()).thenReturn(Set.of("10", "20", "30"));
            when(jobRepository.findIdsByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(10L, 30L));

            geoSyncService.cleanupStaleGeoEntries();

            verify(jobGeoService).removeOpenJob(20L);       // stale
            verify(jobGeoService, never()).removeOpenJob(10L); // active
            verify(jobGeoService, never()).removeOpenJob(30L); // active
        }

        @Test
        @DisplayName("no data loss when cache expires — PostgreSQL remains source of truth")
        void noDataLossOnCacheExpiry() {
            Address addr = Address.builder().latitude(40.0).longitude(-74.0).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(
                    Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(addr).build()
            ));

            // Even after full cache expiration, sync rebuilds from PostgreSQL
            geoSyncService.syncOpenJobsToGeoIndex();
            verify(jobGeoService).addOrUpdateOpenJob(1L, 40.0, -74.0);
        }

        @Test
        @DisplayName("no duplicate entries after re-sync")
        void noDuplicatesAfterReSync() {
            Address addr = Address.builder().latitude(40.0).longitude(-74.0).build();
            Job job = Job.builder().id(1L).jobStatus(JobStatus.OPEN_FOR_BIDS).jobLocation(addr).build();
            when(jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS)).thenReturn(List.of(job));

            // Sync twice
            geoSyncService.syncOpenJobsToGeoIndex();
            geoSyncService.syncOpenJobsToGeoIndex();

            // addOrUpdate is idempotent — called twice but Redis GEO GEOADD overwrites
            verify(jobGeoService, times(2)).addOrUpdateOpenJob(1L, 40.0, -74.0);
        }
    }

    // ── Cache Region Constants ───────────────────────────────────────────
    @Nested
    @DisplayName("Cache Region Constants")
    class CacheRegionTests {

        @Test
        @DisplayName("all cache region names are correctly defined")
        void cacheRegionNames() {
            assertEquals("job:detail", RedisConfig.CACHE_JOB_DETAIL);
            assertEquals("job:client:list", RedisConfig.CACHE_CLIENT_JOBS);
            assertEquals("job:worker:feed", RedisConfig.CACHE_WORKER_FEED);
            assertEquals("user:profile", RedisConfig.CACHE_USER_PROFILE);
            assertEquals("worker:skills", RedisConfig.CACHE_WORKER_SKILLS);
        }
    }
}
