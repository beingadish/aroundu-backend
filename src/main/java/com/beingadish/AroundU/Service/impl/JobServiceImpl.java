package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Config.RedisConfig;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobFilterRequest;
import com.beingadish.AroundU.DTO.Job.JobStatusUpdateRequest;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.DTO.Job.WorkerJobFeedRequest;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Skill;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Exceptions.Job.JobNotFoundException;
import com.beingadish.AroundU.Exceptions.Job.JobValidationException;
import com.beingadish.AroundU.Mappers.Job.JobMapper;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Service.JobGeoService;
import com.beingadish.AroundU.Service.JobService;
import com.beingadish.AroundU.Service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobServiceImpl implements JobService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(
            JobStatus.CREATED,
            JobStatus.OPEN_FOR_BIDS,
            JobStatus.BID_SELECTED_AWAITING_HANDSHAKE,
            JobStatus.READY_TO_START,
            JobStatus.IN_PROGRESS
    );

    private static final List<JobStatus> PAST_STATUSES = List.of(JobStatus.COMPLETED, JobStatus.CANCELLED);

    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final SkillRepository skillRepository;
    private final WorkerReadRepository workerReadRepository;
    private final JobMapper jobMapper;
    private final JobGeoService jobGeoService;
    private final MetricsService metricsService;

    @Override
    @CacheEvict(value = {RedisConfig.CACHE_JOB_DETAIL, RedisConfig.CACHE_CLIENT_JOBS, RedisConfig.CACHE_WORKER_FEED}, allEntries = true)
    public JobDetailDTO createJob(Long clientId, JobCreateRequest request) {
        return metricsService.recordTimer(metricsService.getJobCreationTimer(), () -> {
            Client client = clientRepository.findById(clientId).orElseThrow(() -> new JobValidationException("Client not found"));
            Address location = addressRepository.findById(request.getJobLocationId()).orElseThrow(() -> new JobValidationException("Job location not found"));
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.getRequiredSkillIds()));
            if (skills.isEmpty()) {
                throw new JobValidationException("At least one required skill must be provided");
            }
            Job job = jobMapper.toEntity(request, location, skills, client);
            job.setJobStatus(JobStatus.OPEN_FOR_BIDS);
            Job saved = jobRepository.save(job);
            if (saved.getJobStatus() == JobStatus.OPEN_FOR_BIDS) {
                jobGeoService.addOrUpdateOpenJob(saved.getId(), location.getLatitude(), location.getLongitude());
            }
            metricsService.getJobsCreatedCounter().increment();
            metricsService.incrementActiveJobs();
            log.info("Created job id={} for client={}", saved.getId(), clientId);
            return jobMapper.toDetailDto(saved);
        });
    }

    @Override
    @CacheEvict(value = {RedisConfig.CACHE_JOB_DETAIL, RedisConfig.CACHE_CLIENT_JOBS, RedisConfig.CACHE_WORKER_FEED}, allEntries = true)
    public JobDetailDTO updateJob(Long jobId, Long clientId, JobUpdateRequest request) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException("Job not found"));
        if (!Objects.equals(job.getCreatedBy().getId(), clientId)) {
            throw new AccessDeniedException("You cannot edit another client's job");
        }
        Address location = null;
        if (request.getJobLocationId() != null) {
            location = addressRepository.findById(request.getJobLocationId()).orElseThrow(() -> new JobValidationException("Job location not found"));
        }
        Set<Skill> skills = null;
        if (request.getRequiredSkillIds() != null) {
            skills = new HashSet<>(skillRepository.findAllById(request.getRequiredSkillIds()));
            if (skills.isEmpty()) {
                throw new JobValidationException("Required skills cannot be empty");
            }
        }
        request.setJobStatus(null); // enforce status changes through updateJobStatus
        jobMapper.updateEntity(request, job, location, skills);
        Job saved = jobRepository.save(job);
        if (location != null && saved.getJobStatus() == JobStatus.OPEN_FOR_BIDS) {
            jobGeoService.addOrUpdateOpenJob(saved.getId(), location.getLatitude(), location.getLongitude());
        }
        return jobMapper.toDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailDTO getJobDetail(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException("Job not found"));
        return jobMapper.toDetailDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobSummaryDTO> listJobs(String city, String area, List<Long> skillIds) {
        List<Job> jobs;
        if (skillIds == null || skillIds.isEmpty()) {
            jobs = jobRepository.searchByLocation(city, area);
        } else {
            jobs = jobRepository.searchByLocationAndSkills(city, area, skillIds);
        }
        return jobMapper.toSummaryList(jobs);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_CLIENT_JOBS, key = "#clientId + ':' + #filterRequest.hashCode()")
    public Page<JobSummaryDTO> getClientJobs(Long clientId, JobFilterRequest filterRequest) {
        int page = Optional.ofNullable(filterRequest.getPage()).orElse(0);
        int size = Optional.ofNullable(filterRequest.getSize()).orElse(20);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<JobStatus> statuses = (filterRequest.getStatuses() == null || filterRequest.getStatuses().isEmpty()) ? ACTIVE_STATUSES : filterRequest.getStatuses();

        Page<Job> jobPage;
        if (filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
            LocalDate start = filterRequest.getStartDate();
            LocalDate end = filterRequest.getEndDate();
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
            jobPage = jobRepository.findByCreatedByIdAndJobStatusInAndCreatedAtBetween(clientId, statuses, startDateTime, endDateTime, pageable);
        } else {
            jobPage = jobRepository.findByCreatedByIdAndJobStatusIn(clientId, statuses, pageable);
        }
        return jobPage.map(jobMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_CLIENT_JOBS, key = "#clientId + ':past:' + #page + ':' + #size")
    public Page<JobSummaryDTO> getClientPastJobs(Long clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Job> jobPage = jobRepository.findByCreatedByIdAndJobStatusIn(clientId, PAST_STATUSES, pageable);
        return jobPage.map(jobMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailDTO getJobForClient(Long jobId, Long clientId) {
        Job job = jobRepository.findByIdAndCreatedById(jobId, clientId).orElseThrow(() -> new JobNotFoundException("Job not found for client"));
        return jobMapper.toDetailDto(job);
    }

    @Override
    @CacheEvict(value = {RedisConfig.CACHE_JOB_DETAIL, RedisConfig.CACHE_CLIENT_JOBS, RedisConfig.CACHE_WORKER_FEED}, allEntries = true)
    public JobDetailDTO updateJobStatus(Long jobId, Long clientId, JobStatusUpdateRequest request) {
        Job job = jobRepository.findByIdAndCreatedById(jobId, clientId).orElseThrow(() -> new JobNotFoundException("Job not found for client"));
        validateStatusTransition(job.getJobStatus(), request.getNewStatus());
        JobStatus oldStatus = job.getJobStatus();
        job.setJobStatus(request.getNewStatus());
        Job saved = jobRepository.save(job);
        handleGeoOnStatusChange(saved, oldStatus, request.getNewStatus());
        // Record metrics for terminal statuses
        if (request.getNewStatus() == JobStatus.COMPLETED) {
            metricsService.getJobsCompletedCounter().increment();
            metricsService.decrementActiveJobs();
        } else if (request.getNewStatus() == JobStatus.CANCELLED) {
            metricsService.getJobsCancelledCounter().increment();
            metricsService.decrementActiveJobs();
        }
        log.info("Job id={} status updated from {} to {} by client {}", jobId, oldStatus, request.getNewStatus(), clientId);
        return jobMapper.toDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_WORKER_FEED, key = "#workerId + ':' + #request.hashCode()")
    public Page<JobSummaryDTO> getWorkerFeed(Long workerId, WorkerJobFeedRequest request) {
        Worker worker = workerReadRepository.findById(workerId).orElseThrow(() -> new JobValidationException("Worker not found"));
        double radius = Optional.ofNullable(request.getRadiusKm()).orElse(25.0);
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(20);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Long> geoJobIds = jobGeoService.findNearbyOpenJobs(
                worker.getCurrentAddress() != null ? worker.getCurrentAddress().getLatitude() : null,
                worker.getCurrentAddress() != null ? worker.getCurrentAddress().getLongitude() : null,
                radius,
                size * 3
        );

        Page<Job> jobsPage;
        if (!geoJobIds.isEmpty()) {
            jobsPage = jobRepository.findByIdInAndJobStatus(geoJobIds, JobStatus.OPEN_FOR_BIDS, pageable);
        } else {
            jobsPage = jobRepository.findOpenJobsBySkills(JobStatus.OPEN_FOR_BIDS, request.getSkillIds(), pageable);
        }

        List<Job> filtered = new ArrayList<>();
        for (Job job : jobsPage.getContent()) {
            if (request.getSkillIds() == null || request.getSkillIds().isEmpty()) {
                filtered.add(job);
                continue;
            }
            boolean hasSkill = job.getSkillSet().stream().anyMatch(skill -> request.getSkillIds().contains(skill.getId()));
            if (hasSkill) {
                filtered.add(job);
            }
        }

        Page<Job> finalPage = new PageImpl<>(filtered, pageable, jobsPage.getTotalElements());
        return finalPage.map(jobMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailDTO getJobForWorker(Long jobId, Long workerId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException("Job not found"));
        if (job.getJobStatus() == JobStatus.OPEN_FOR_BIDS || (job.getAssignedTo() != null && Objects.equals(job.getAssignedTo().getId(), workerId))) {
            return jobMapper.toDetailDto(job);
        }
        throw new AccessDeniedException("Worker cannot view this job");
    }

    @Override
    @CacheEvict(value = {RedisConfig.CACHE_JOB_DETAIL, RedisConfig.CACHE_CLIENT_JOBS, RedisConfig.CACHE_WORKER_FEED}, allEntries = true)
    public void deleteJob(Long jobId, Long clientId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException("Job not found"));
        if (!Objects.equals(job.getCreatedBy().getId(), clientId)) {
            throw new AccessDeniedException("You cannot delete another client's job");
        }

        boolean wasOpen = job.getJobStatus() == JobStatus.OPEN_FOR_BIDS;
        jobRepository.delete(job);
        if (wasOpen) {
            jobGeoService.removeOpenJob(jobId);
        }
        log.info("Deleted job id={} for client={}", jobId, clientId);
    }

    private void validateStatusTransition(JobStatus current, JobStatus target) {
        if (current == target) {
            throw new JobValidationException("Job is already in status " + target);
        }
        boolean allowed = switch (current) {
            case CREATED ->
                target == JobStatus.OPEN_FOR_BIDS;
            case OPEN_FOR_BIDS ->
                target == JobStatus.BID_SELECTED_AWAITING_HANDSHAKE || target == JobStatus.CANCELLED;
            case BID_SELECTED_AWAITING_HANDSHAKE ->
                target == JobStatus.READY_TO_START || target == JobStatus.CANCELLED;
            case READY_TO_START ->
                target == JobStatus.IN_PROGRESS || target == JobStatus.CANCELLED;
            case IN_PROGRESS ->
                target == JobStatus.COMPLETED || target == JobStatus.CANCELLED;
            case COMPLETED, CANCELLED ->
                false;
        };
        if (!allowed) {
            throw new JobValidationException("Invalid status transition from " + current + " to " + target);
        }
    }

    private void handleGeoOnStatusChange(Job job, JobStatus oldStatus, JobStatus newStatus) {
        boolean wasOpen = oldStatus == JobStatus.OPEN_FOR_BIDS;
        boolean nowOpen = newStatus == JobStatus.OPEN_FOR_BIDS;
        if (wasOpen && !nowOpen) {
            jobGeoService.removeOpenJob(job.getId());
        } else if (!wasOpen && nowOpen && job.getJobLocation() != null) {
            jobGeoService.addOrUpdateOpenJob(job.getId(), job.getJobLocation().getLatitude(), job.getJobLocation().getLongitude());
        }
    }
}
