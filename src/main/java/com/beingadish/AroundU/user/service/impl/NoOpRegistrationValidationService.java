package com.beingadish.AroundU.user.service.impl;

import com.beingadish.AroundU.user.service.RegistrationValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link RegistrationValidationService} used under the
 * {@code test} profile where Redis/Redisson is not available.
 */
@Service
@Profile("test")
@Slf4j
public class NoOpRegistrationValidationService implements RegistrationValidationService {

    @Override
    public boolean isEmailAlreadyRegistered(String email) {
        log.debug("NoOp: skipping Bloom filter email check for {}", email);
        return false;
    }

    @Override
    public void recordRegistration(String email) {
        log.debug("NoOp: skipping Bloom filter email recording for {}", email);
    }
}
