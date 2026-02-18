package com.beingadish.AroundU.Service;

/**
 * Bloom-filter-backed pre-check for email registration uniqueness.
 * <p>
 * Provides a fast probabilistic check before hitting the database. If the Bloom
 * filter says "not present", the email is definitely new. If it says "possibly
 * present", a database query confirms.
 */
public interface RegistrationValidationService {

    /**
     * Check whether an email is likely already registered.
     *
     * @param email the email to check (case-insensitive)
     * @return {@code true} if the email is already registered (confirmed by
     * DB), {@code false} if it is available
     */
    boolean isEmailAlreadyRegistered(String email);

    /**
     * Record a successful registration in the Bloom filter.
     *
     * @param email the newly registered email
     */
    void recordRegistration(String email);
}
