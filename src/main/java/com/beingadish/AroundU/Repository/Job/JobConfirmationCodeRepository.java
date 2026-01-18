package com.beingadish.AroundU.Repository.Job;

import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobConfirmationCodeRepository extends JpaRepository<JobConfirmationCode, Long> {
    Optional<JobConfirmationCode> findByJob(Job job);
}
