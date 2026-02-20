package com.beingadish.AroundU.job.service;

import com.beingadish.AroundU.job.entity.JobConfirmationCode;

/**
 * Coordinates generation and validation of start/release confirmation codes that guard job execution.
 */
public interface JobCodeService {

    /**
     * Creates one-time start and release codes for a job once the client has selected a bid.
     * If codes already exist for the job, the existing record is returned.
     */
    JobConfirmationCode generateCodes(Long jobId, Long clientId);

    /**
     * Confirms a worker-entered start code, transitioning the job to in-progress once the assigned worker matches.
     */
    JobConfirmationCode verifyStartCode(Long jobId, Long workerId, String code);

    /**
     * Confirms the client-provided release code to mark the job complete and release escrow.
     */
    JobConfirmationCode verifyReleaseCode(Long jobId, Long clientId, String code);
}
