package com.beingadish.AroundU.Repository.Worker;

import com.beingadish.AroundU.Entities.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface WorkerReadRepository extends Repository<Worker, Long> {
    Optional<Worker> findById(Long id);

    Optional<Worker> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<Worker> findByPhoneNumber(String phoneNumber);

    Boolean existsByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT w FROM Worker w", countQuery = "SELECT count(w) FROM Worker w")
    Page<Worker> findAll(Pageable pageable);
}
