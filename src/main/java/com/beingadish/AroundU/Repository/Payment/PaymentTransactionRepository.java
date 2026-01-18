package com.beingadish.AroundU.Repository.Payment;

import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByJob(Job job);
}
