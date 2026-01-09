package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;

import java.util.List;

public interface JobService {
	JobDetailDTO createJob(Long clientId, JobCreateRequest request);

	JobDetailDTO updateJob(Long jobId, JobUpdateRequest request);

	JobDetailDTO getJobDetail(Long jobId);

	List<JobSummaryDTO> listJobs(String city, String area, List<Long> skillIds);
}
