package com.beingadish.AroundU.job.service;

import com.beingadish.AroundU.job.dto.JobCreateRequest;
import com.beingadish.AroundU.job.dto.JobDetailDTO;
import com.beingadish.AroundU.job.dto.JobFilterRequest;
import com.beingadish.AroundU.job.dto.JobStatusUpdateRequest;
import com.beingadish.AroundU.job.dto.JobSummaryDTO;
import com.beingadish.AroundU.job.dto.JobUpdateRequest;
import com.beingadish.AroundU.job.dto.WorkerJobFeedRequest;
import com.beingadish.AroundU.common.util.PageResponse;

import java.util.List;

public interface JobService {

    JobDetailDTO createJob(Long clientId, JobCreateRequest request);

    JobDetailDTO updateJob(Long jobId, Long clientId, JobUpdateRequest request);

    JobDetailDTO getJobDetail(Long jobId);

    List<JobSummaryDTO> listJobs(String city, String area, List<Long> skillIds);

    PageResponse<JobSummaryDTO> getClientJobs(Long clientId, JobFilterRequest filterRequest);

    PageResponse<JobSummaryDTO> getClientPastJobs(Long clientId, int page, int size);

    JobDetailDTO getJobForClient(Long jobId, Long clientId);

    JobDetailDTO updateJobStatus(Long jobId, Long clientId, JobStatusUpdateRequest request);

    /**
     * Worker updates job status (e.g. mark as IN_PROGRESS or
     * COMPLETED_PENDING_PAYMENT).
     */
    JobDetailDTO updateJobStatusByWorker(Long jobId, Long workerId, JobStatusUpdateRequest request);

    PageResponse<JobSummaryDTO> getWorkerFeed(Long workerId, WorkerJobFeedRequest request);

    JobDetailDTO getJobForWorker(Long jobId, Long workerId);

    /**
     * Worker cancels an accepted/in-progress job. Triggers cancellation
     * penalty.
     */
    JobDetailDTO cancelJobByWorker(Long jobId, Long workerId);

    void deleteJob(Long jobId, Long clientId);
}
