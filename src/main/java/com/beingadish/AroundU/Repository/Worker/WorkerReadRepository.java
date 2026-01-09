package com.beingadish.AroundU.Repository.Worker;

import com.beingadish.AroundU.Entities.Worker;

import java.util.Optional;

public interface WorkerReadRepository {
    // Check if email already exists (for registration validation)
    boolean existsByEmail(String email);

    // Find worker by email (useful for login/authentication later)
    Optional<Worker> findByEmail(String email);

    // Find worker by phone number (additional validation)
    Optional<Worker> findByPhoneNumber(String phoneNumber);

    // Check if phone number exists
    boolean existsByPhoneNumber(String phoneNumber);
}
