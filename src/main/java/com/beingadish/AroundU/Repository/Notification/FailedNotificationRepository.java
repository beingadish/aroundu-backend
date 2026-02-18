package com.beingadish.AroundU.Repository.Notification;

import com.beingadish.AroundU.Entities.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for persisting and querying failed notification records.
 */
@Repository
public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {

    List<FailedNotification> findByResolvedFalseAndRetryCountLessThan(int maxRetries);

    List<FailedNotification> findByJobId(Long jobId);

    long countByResolvedFalse();
}
