package com.beingadish.AroundU.Repository.Worker;

import com.beingadish.AroundU.Entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    // Check if email already exists (for registration validation)
    boolean existsByEmail(String email);

    // Find worker by email (useful for login/authentication later)
    Optional<Worker> findByEmail(String email);

    // Find worker by phone number (additional validation)
    Optional<Worker> findByPhoneNumber(String phoneNumber);

    // Check if phone number exists
    boolean existsByPhoneNumber(String phoneNumber);
}
