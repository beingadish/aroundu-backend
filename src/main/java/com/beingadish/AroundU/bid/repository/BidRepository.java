package com.beingadish.AroundU.bid.repository;

import com.beingadish.AroundU.bid.entity.Bid;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.user.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByJob(Job job);

    List<Bid> findByWorker(Worker worker);

    long countByJobId(Long jobId);

    boolean existsByWorkerIdAndJobId(Long workerId, Long jobId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Batch reject all bids for a job except the selected bid.
     */
    @Modifying
    @Query("UPDATE Bid b SET b.status = com.beingadish.AroundU.common.constants.enums.BidStatus.REJECTED WHERE b.job = :job AND b.id <> :selectedBidId")
    int rejectOtherBids(@Param("job") Job job, @Param("selectedBidId") Long selectedBidId);

    /**
     * Batch count bids for multiple jobs at once.
     */
    @Query("SELECT b.job.id, COUNT(b) FROM Bid b WHERE b.job.id IN :jobIds GROUP BY b.job.id")
    List<Object[]> countByJobIds(@Param("jobIds") List<Long> jobIds);
}
