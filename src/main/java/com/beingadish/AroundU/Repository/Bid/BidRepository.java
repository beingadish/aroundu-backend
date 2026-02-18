package com.beingadish.AroundU.Repository.Bid;

import com.beingadish.AroundU.Entities.Bid;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByJob(Job job);

    List<Bid> findByWorker(Worker worker);

    long countByJobId(Long jobId);

    boolean existsByWorkerIdAndJobId(Long workerId, Long jobId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
