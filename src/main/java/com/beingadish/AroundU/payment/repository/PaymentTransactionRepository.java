package com.beingadish.AroundU.payment.repository;

import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByJob(Job job);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM PaymentTransaction p "
            + "WHERE p.status = :status AND p.createdAt BETWEEN :start AND :end")
    Double sumAmountByStatusAndCreatedAtBetween(@Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
