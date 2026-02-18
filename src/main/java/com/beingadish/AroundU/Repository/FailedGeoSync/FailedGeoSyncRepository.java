package com.beingadish.AroundU.Repository.FailedGeoSync;

import com.beingadish.AroundU.Entities.FailedGeoSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedGeoSyncRepository extends JpaRepository<FailedGeoSync, Long> {

    /**
     * Find all unresolved failures with fewer than the given number of retries,
     * ordered by creation time (oldest first).
     */
    List<FailedGeoSync> findByResolvedFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetries);

    /**
     * Check if there is already an unresolved failure for a specific job and
     * operation type.
     */
    boolean existsByJobIdAndOperationAndResolvedFalse(Long jobId, FailedGeoSync.SyncOperation operation);

    /**
     * Mark all failures for a job as resolved (e.g. after manual fix or job
     * deletion).
     */
    List<FailedGeoSync> findByJobIdAndResolvedFalse(Long jobId);
}
