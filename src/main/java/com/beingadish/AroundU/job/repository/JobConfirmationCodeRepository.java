package com.beingadish.AroundU.job.repository;

import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.entity.JobConfirmationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobConfirmationCodeRepository extends JpaRepository<JobConfirmationCode, Long> {
    Optional<JobConfirmationCode> findByJob(Job job);
}
