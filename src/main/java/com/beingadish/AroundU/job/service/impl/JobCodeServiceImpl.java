package com.beingadish.AroundU.job.service.impl;

import com.beingadish.AroundU.common.constants.enums.JobCodeStatus;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.entity.JobConfirmationCode;
import com.beingadish.AroundU.job.mapper.JobConfirmationCodeMapper;
import com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.job.service.JobCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Implements OTP-based job confirmation with secure random generation, expiry,
 * attempt tracking, and brute-force protection.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JobCodeServiceImpl implements JobCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JobRepository jobRepository;
    private final JobConfirmationCodeRepository codeRepository;
    private final JobConfirmationCodeMapper codeMapper;

    @Value("${otp.expiry-minutes:30}")
    private int otpExpiryMinutes;

    @Override
    public JobConfirmationCode generateCodes(Long jobId, Long clientId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        validateClientOwnership(job, clientId);

        if (job.getJobStatus() != JobStatus.BID_SELECTED_AWAITING_HANDSHAKE
                && job.getJobStatus() != JobStatus.READY_TO_START) {
            throw new IllegalStateException("Codes can only be generated after bid selection");
        }

        // Return existing codes if present
        return codeRepository.findByJob(job).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            String startOtp = generateSecureOtp();
            String releaseOtp = generateSecureOtp();

            JobConfirmationCode code = JobConfirmationCode.builder()
                    .job(job)
                    .startCode(startOtp)
                    .releaseCode(releaseOtp)
                    .status(JobCodeStatus.START_PENDING)
                    .startCodeGeneratedAt(now)
                    .startCodeExpiresAt(now.plusMinutes(otpExpiryMinutes))
                    .releaseCodeGeneratedAt(now)
                    .releaseCodeExpiresAt(now.plusMinutes(otpExpiryMinutes))
                    .startCodeAttempts(0)
                    .releaseCodeAttempts(0)
                    .build();

            log.info("Generated OTP codes for jobId={}", jobId);
            return codeRepository.save(code);
        });
    }

    @Override
    public JobConfirmationCode regenerateCodes(Long jobId, Long clientId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        validateClientOwnership(job, clientId);

        JobConfirmationCode existing = codeRepository.findByJob(job)
                .orElseThrow(() -> new EntityNotFoundException("No codes exist for this job"));

        // Rate-limit: prevent regeneration within 1 minute
        if (existing.getStartCodeGeneratedAt() != null
                && existing.getStartCodeGeneratedAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("OTP can only be regenerated once per minute");
        }

        LocalDateTime now = LocalDateTime.now();

        // Regenerate start code only if start is still pending
        if (existing.getStatus() == JobCodeStatus.START_PENDING) {
            existing.setStartCode(generateSecureOtp());
            existing.setStartCodeGeneratedAt(now);
            existing.setStartCodeExpiresAt(now.plusMinutes(otpExpiryMinutes));
            existing.setStartCodeAttempts(0);
        }

        // Always regenerate release code
        existing.setReleaseCode(generateSecureOtp());
        existing.setReleaseCodeGeneratedAt(now);
        existing.setReleaseCodeExpiresAt(now.plusMinutes(otpExpiryMinutes));
        existing.setReleaseCodeAttempts(0);

        log.info("Regenerated OTP codes for jobId={}", jobId);
        return codeRepository.save(existing);
    }

    @Override
    public JobConfirmationCode verifyStartCode(Long jobId, Long workerId, String code) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        JobConfirmationCode confirmation = codeRepository.findByJob(job)
                .orElseThrow(() -> new EntityNotFoundException("Codes not generated"));

        if (job.getAssignedTo() == null) {
            throw new IllegalStateException("Job has no assigned worker");
        }
        if (!job.getAssignedTo().getId().equals(workerId)) {
            throw new IllegalStateException("Worker not assigned to this job");
        }
        if (job.getJobStatus() != JobStatus.READY_TO_START) {
            throw new IllegalStateException("Job is not ready to start");
        }
        if (confirmation.isStartCodeLocked()) {
            throw new IllegalStateException("Start code verification locked due to too many failed attempts. Please regenerate.");
        }
        if (confirmation.isStartCodeExpired()) {
            throw new IllegalStateException("Start code has expired. Please request a new code.");
        }

        if (!confirmation.getStartCode().equals(code)) {
            confirmation.setStartCodeAttempts(confirmation.getStartCodeAttempts() + 1);
            codeRepository.save(confirmation);
            int remaining = JobConfirmationCode.MAX_ATTEMPTS - confirmation.getStartCodeAttempts();
            throw new IllegalArgumentException("Invalid start code. " + remaining + " attempts remaining.");
        }

        confirmation.setStatus(JobCodeStatus.RELEASE_PENDING);
        confirmation.setStartCodeAttempts(0); // reset on success
        job.setJobStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        log.info("Start OTP verified for jobId={} by workerId={}", jobId, workerId);
        return codeRepository.save(confirmation);
    }

    @Override
    public JobConfirmationCode verifyReleaseCode(Long jobId, Long clientId, String code) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        validateClientOwnership(job, clientId);

        if (job.getAssignedTo() == null) {
            throw new IllegalStateException("Job has no assigned worker");
        }

        JobConfirmationCode confirmation = codeRepository.findByJob(job)
                .orElseThrow(() -> new EntityNotFoundException("Codes not generated"));

        if (confirmation.getStatus() != JobCodeStatus.RELEASE_PENDING) {
            throw new IllegalStateException("Start code not yet confirmed");
        }
        if (job.getJobStatus() != JobStatus.IN_PROGRESS
                && job.getJobStatus() != JobStatus.COMPLETED_PENDING_PAYMENT) {
            throw new IllegalStateException("Job is not in a state that allows release code verification");
        }
        if (confirmation.isReleaseCodeLocked()) {
            throw new IllegalStateException("Release code verification locked due to too many failed attempts. Please regenerate.");
        }
        if (confirmation.isReleaseCodeExpired()) {
            throw new IllegalStateException("Release code has expired. Please request a new code.");
        }

        if (!confirmation.getReleaseCode().equals(code)) {
            confirmation.setReleaseCodeAttempts(confirmation.getReleaseCodeAttempts() + 1);
            codeRepository.save(confirmation);
            int remaining = JobConfirmationCode.MAX_ATTEMPTS - confirmation.getReleaseCodeAttempts();
            throw new IllegalArgumentException("Invalid release code. " + remaining + " attempts remaining.");
        }

        confirmation.setStatus(JobCodeStatus.COMPLETED);
        confirmation.setReleaseCodeAttempts(0);
        job.setJobStatus(JobStatus.COMPLETED_PENDING_PAYMENT);
        jobRepository.save(job);

        log.info("Release OTP verified for jobId={} by clientId={}", jobId, clientId);
        return codeRepository.save(confirmation);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Generates a cryptographically secure 6-digit OTP.
     */
    private String generateSecureOtp() {
        int otp = SECURE_RANDOM.nextInt(900_000) + 100_000; // 100000–999999
        return String.valueOf(otp);
    }

    private void validateClientOwnership(Job job, Long clientId) {
        if (!job.getCreatedBy().getId().equals(clientId)) {
            throw new IllegalStateException("Client does not own this job");
        }
    }
}
