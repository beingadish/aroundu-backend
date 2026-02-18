package com.beingadish.AroundU.user.service.impl;

import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import com.beingadish.AroundU.bid.service.BloomFilterMetricsService;
import com.beingadish.AroundU.user.service.RegistrationValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Bloom-filter-backed email registration pre-check.
 * <p>
 * Flow:
 * <ol>
 * <li>Normalise email to lowercase</li>
 * <li>Check Bloom filter – if "definitely not present", email is new (fast
 * path)</li>
 * <li>If "possibly present", query Client and Worker repositories</li>
 * <li>If DB confirms the email exists → return {@code true}</li>
 * <li>If false positive → log, return {@code false}</li>
 * <li>After successful registration call {@link #recordRegistration}</li>
 * </ol>
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class RegistrationValidationServiceImpl implements RegistrationValidationService {

    private final RBloomFilter<String> emailRegistrationBloomFilter;
    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerReadRepository;
    private final BloomFilterMetricsService bloomFilterMetricsService;

    @Override
    public boolean isEmailAlreadyRegistered(String email) {
        String normalised = normalise(email);
        String key = "email:" + normalised;

        if (!emailRegistrationBloomFilter.contains(key)) {
            // Bloom filter says "definitely not present" – fast path
            log.debug("Email definitely not registered (Bloom): {}", normalised);
            return false;
        }

        log.debug("Bloom filter reports email possibly registered, verifying in DB: {}", normalised);

        boolean existsInClient = Boolean.TRUE.equals(clientReadRepository.existsByEmail(normalised));
        boolean existsInWorker = Boolean.TRUE.equals(workerReadRepository.existsByEmail(normalised));

        if (existsInClient || existsInWorker) {
            log.info("Email confirmed registered in database: {}", normalised);
            return true;
        }

        // False positive
        log.info("Bloom filter false positive for email check: {}", normalised);
        bloomFilterMetricsService.recordFalsePositive("email");
        return false;
    }

    @Override
    public void recordRegistration(String email) {
        String key = "email:" + normalise(email);
        emailRegistrationBloomFilter.add(key);
        log.debug("Recorded email in Bloom filter: {}", normalise(email));
    }

    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
