package com.beingadish.AroundU.job.service;

import com.beingadish.AroundU.job.entity.JobConfirmationCode;

/**
 * Coordinates generation, regeneration, and validation of 6-digit OTP codes
 * that guard job start and release workflows.
 * <p>
 * OTPs are generated using {@link java.security.SecureRandom}, expire after a
 * configurable window (default 30 minutes), and are protected against
 * brute-force via attempt counting (max 5 failed attempts per code phase).
 */
public interface JobCodeService {

    /**
     * Generates 6-digit start and release OTPs for a job once the client has
     * selected a bid. If codes already exist for the job, the existing record
     * is returned.
     *
     * @param jobId the job ID
     * @param clientId the owning client ID
     * @return the created or existing confirmation codes
     */
    JobConfirmationCode generateCodes(Long jobId, Long clientId);

    /**
     * Regenerates OTPs for a job, invalidating any previous codes.
     * Rate-limited: callers should not regenerate more than once per minute.
     *
     * @param jobId the job ID
     * @param clientId the owning client ID
     * @return the new confirmation codes
     */
    JobConfirmationCode regenerateCodes(Long jobId, Long clientId);

    /**
     * Verifies the worker-entered start OTP. On success, transitions the job to
     * IN_PROGRESS. Tracks failed attempts and locks after
     * {@link JobConfirmationCode#MAX_ATTEMPTS} failures.
     *
     * @param jobId the job ID
     * @param workerId the assigned worker ID
     * @param code the 6-digit start OTP
     * @return the updated confirmation codes
     */
    JobConfirmationCode verifyStartCode(Long jobId, Long workerId, String code);

    /**
     * Verifies the worker-entered release OTP. The client shares this code with
     * the worker verbally once they are satisfied with the work. On success,
     * marks the job as COMPLETED_PENDING_PAYMENT. Tracks failed attempts and
     * locks after {@link JobConfirmationCode#MAX_ATTEMPTS} failures.
     *
     * @param jobId the job ID
     * @param workerId the assigned worker ID
     * @param code the 6-digit release OTP
     * @return the updated confirmation codes
     */
    JobConfirmationCode verifyReleaseCode(Long jobId, Long workerId, String code);

    /**
     * Fetches existing confirmation codes for a job. Only the code relevant to
     * the current step is returned: start code when START_PENDING, release code
     * when RELEASE_PENDING. Returns an empty optional if no codes have been
     * generated yet.
     *
     * @param jobId the job ID
     * @param clientId the owning client ID
     * @return the confirmation codes record
     */
    JobConfirmationCode fetchCodes(Long jobId, Long clientId);
}
