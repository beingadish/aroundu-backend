package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobFilterRequest;
import com.beingadish.AroundU.DTO.Job.JobStatusUpdateRequest;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.DTO.Job.WorkerJobFeedRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface JobService {
	JobDetailDTO createJob(Long clientId, JobCreateRequest request);

	JobDetailDTO updateJob(Long jobId, Long clientId, JobUpdateRequest request);

	JobDetailDTO getJobDetail(Long jobId);

	List<JobSummaryDTO> listJobs(String city, String area, List<Long> skillIds);

	Page<JobSummaryDTO> getClientJobs(Long clientId, JobFilterRequest filterRequest);

	Page<JobSummaryDTO> getClientPastJobs(Long clientId, int page, int size);

	JobDetailDTO getJobForClient(Long jobId, Long clientId);

	JobDetailDTO updateJobStatus(Long jobId, Long clientId, JobStatusUpdateRequest request);

	Page<JobSummaryDTO> getWorkerFeed(Long workerId, WorkerJobFeedRequest request);

	JobDetailDTO getJobForWorker(Long jobId, Long workerId);

	void deleteJob(Long jobId, Long clientId);
}
