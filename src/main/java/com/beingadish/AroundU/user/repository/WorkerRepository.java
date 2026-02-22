package com.beingadish.AroundU.user.repository;

import com.beingadish.AroundU.user.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT w FROM Worker w WHERE w.deleted = false AND w.lastLoginAt < :cutoff")
    List<Worker> findInactiveWorkersBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT w FROM Worker w WHERE w.blockedUntil IS NOT NULL AND w.blockedUntil <= :now")
    List<Worker> findBlockedWorkersWithExpiredPenalty(@Param("now") LocalDateTime now);
}
