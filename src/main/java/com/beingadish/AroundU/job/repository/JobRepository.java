package com.beingadish.AroundU.job.repository;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("select j from Job j where (:city is null or lower(j.jobLocation.city) = lower(:city)) and (:area is null or lower(j.jobLocation.area) = lower(:area))")
    List<Job> searchByLocation(@Param("city") String city, @Param("area") String area);

    @Query("select distinct j from Job j left join j.skillSet s where (:city is null or lower(j.jobLocation.city) = lower(:city)) and (:area is null or lower(j.jobLocation.area) = lower(:area)) and (:skillIds is null or s.id in :skillIds)")
    List<Job> searchByLocationAndSkills(@Param("city") String city, @Param("area") String area, @Param("skillIds") List<Long> skillIds);

    @EntityGraph(attributePaths = {"skillSet", "jobLocation", "createdBy"})
    Page<Job> findByCreatedByIdAndJobStatusIn(Long clientId, Collection<JobStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"skillSet", "jobLocation", "createdBy"})
    Page<Job> findByCreatedByIdAndJobStatusInAndCreatedAtBetween(Long clientId, Collection<JobStatus> statuses, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Optional<Job> findByIdAndCreatedById(Long id, Long clientId);

    Optional<Job> findByIdAndAssignedToId(Long id, Long workerId);

    @Query("select distinct j from Job j left join j.skillSet s where j.jobStatus = :status and (:skillIds is null or s.id in :skillIds)")
    Page<Job> findOpenJobsBySkills(@Param("status") JobStatus status, @Param("skillIds") Collection<Long> skillIds, Pageable pageable);

    @EntityGraph(attributePaths = {"skillSet"})
    Page<Job> findByIdInAndJobStatus(Collection<Long> ids, JobStatus status, Pageable pageable);

    List<Job> findByJobStatus(JobStatus status);

    @Query("SELECT j.id FROM Job j WHERE j.jobStatus = :status")
    List<Long> findIdsByJobStatus(@Param("status") JobStatus status);

    List<Job> findTop100ByJobStatusOrderByCreatedAtDesc(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.jobStatus = :status AND "
            + "(j.scheduledStartTime IS NOT NULL AND j.scheduledStartTime < :now "
            + "OR j.scheduledStartTime IS NULL AND j.createdAt < :fallback)")
    List<Job> findExpiredJobs(@Param("status") JobStatus status,
                              @Param("now") LocalDateTime now,
                              @Param("fallback") LocalDateTime fallback);

    @Query("SELECT j FROM Job j WHERE j.jobStatus = :status AND j.createdAt < :before "
            + "AND (SELECT COUNT(b) FROM Bid b WHERE b.job = j) = 0")
    List<Job> findJobsWithZeroBids(@Param("status") JobStatus status,
                                   @Param("before") LocalDateTime before);

    long countByJobStatusAndCreatedAtBetween(JobStatus status, LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Does this worker currently have a non-terminal job (assigned to them)?
     */
    @Query("SELECT COUNT(j) > 0 FROM Job j WHERE j.assignedTo.id = :workerId "
            + "AND j.jobStatus IN :activeStatuses")
    boolean hasActiveJobAsWorker(@Param("workerId") Long workerId,
                                 @Param("activeStatuses") Collection<JobStatus> activeStatuses);

    /**
     * Does this client currently have a non-terminal job that has a worker
     * assigned and is in progress?
     */
    @Query("SELECT COUNT(j) > 0 FROM Job j WHERE j.createdBy.id = :clientId "
            + "AND j.jobStatus = :status")
    boolean hasJobInStatus(@Param("clientId") Long clientId,
                           @Param("status") JobStatus status);
}
