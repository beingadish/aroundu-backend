package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Constants.Enums.JobCodeStatus;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import com.beingadish.AroundU.Mappers.Job.JobConfirmationCodeMapper;
import com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Service.JobCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements guarded workflows for generating and validating job confirmation codes across start and release steps.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JobCodeServiceImpl implements JobCodeService {

    private final JobRepository jobRepository;
    private final JobConfirmationCodeRepository codeRepository;
    private final JobConfirmationCodeMapper codeMapper;

    @Override
    public JobConfirmationCode generateCodes(Long jobId, Long clientId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getCreatedBy().getId().equals(clientId)) {
            throw new IllegalStateException("Client does not own this job");
        }
        if (job.getJobStatus() != JobStatus.BID_SELECTED_AWAITING_HANDSHAKE && job.getJobStatus() != JobStatus.READY_TO_START) {
            throw new IllegalStateException("Codes can only be generated after bid selection");
        }
        JobConfirmationCode existing = codeRepository.findByJob(job).orElse(null);
        if (existing != null) {
            return existing;
        }
        String start = RandomStringUtils.randomNumeric(6);
        String release = RandomStringUtils.randomNumeric(6);
        JobConfirmationCode code = codeMapper.create(job, start, release);
        return codeRepository.save(code);
    }

    @Override
    public JobConfirmationCode verifyStartCode(Long jobId, Long workerId, String code) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        JobConfirmationCode confirmation = codeRepository.findByJob(job).orElseThrow(() -> new EntityNotFoundException("Codes not generated"));
        if (job.getAssignedTo() == null) {
            throw new IllegalStateException("Job has no assigned worker");
        }
        if (!job.getAssignedTo().getId().equals(workerId)) {
            throw new IllegalStateException("Worker not assigned to this job");
        }
        if (job.getJobStatus() != JobStatus.READY_TO_START) {
            throw new IllegalStateException("Job is not ready to start");
        }
        if (!confirmation.getStartCode().equals(code)) {
            throw new IllegalArgumentException("Invalid start code");
        }
        confirmation.setStatus(JobCodeStatus.RELEASE_PENDING);
        job.setJobStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        return codeRepository.save(confirmation);
    }

    @Override
    public JobConfirmationCode verifyReleaseCode(Long jobId, Long clientId, String code) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getCreatedBy().getId().equals(clientId)) {
            throw new IllegalStateException("Client does not own this job");
        }
        if (job.getAssignedTo() == null) {
            throw new IllegalStateException("Job has no assigned worker");
        }
        JobConfirmationCode confirmation = codeRepository.findByJob(job).orElseThrow(() -> new EntityNotFoundException("Codes not generated"));
        if (confirmation.getStatus() != JobCodeStatus.RELEASE_PENDING) {
            throw new IllegalStateException("Start code not confirmed");
        }
        if (job.getJobStatus() != JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Job is not in progress");
        }
        if (!confirmation.getReleaseCode().equals(code)) {
            throw new IllegalArgumentException("Invalid release code");
        }
        confirmation.setStatus(JobCodeStatus.COMPLETED);
        job.setJobStatus(JobStatus.COMPLETED);
        jobRepository.save(job);
        return codeRepository.save(confirmation);
    }
}
