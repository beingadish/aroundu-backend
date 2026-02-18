package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Config.RedisConfig;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.SortDirection;
import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobFilterRequest;
import com.beingadish.AroundU.DTO.Job.JobStatusUpdateRequest;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.DTO.Job.WorkerJobFeedRequest;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.FailedGeoSync;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Skill;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Events.JobModifiedEvent;
import com.beingadish.AroundU.Exceptions.Job.JobNotFoundException;
import com.beingadish.AroundU.Exceptions.Job.JobValidationException;
import com.beingadish.AroundU.Mappers.Job.JobMapper;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.FailedGeoSync.FailedGeoSyncRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Service.JobGeoService;
import com.beingadish.AroundU.Service.JobService;
import com.beingadish.AroundU.Service.MetricsService;
import com.beingadish.AroundU.Utilities.DistanceUtils;
import com.beingadish.AroundU.Utilities.PopularityUtils;
import com.beingadish.AroundU.Utilities.SortValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Comparator;
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
    private final BidRepository bidRepository;
    private final FailedGeoSyncRepository failedGeoSyncRepository;
    private final JobMapper jobMapper;
    private final JobGeoService jobGeoService;
    private final MetricsService metricsService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
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
                safeGeoAdd(saved.getId(), location.getLatitude(), location.getLongitude());
            }
            metricsService.getJobsCreatedCounter().increment();
            metricsService.incrementActiveJobs();
            log.info("Created job id={} for client={}", saved.getId(), clientId);
            eventPublisher.publishEvent(new JobModifiedEvent(saved.getId(), clientId, JobModifiedEvent.Type.CREATED, false));
            return jobMapper.toDetailDto(saved);
        });
    }

    @Override
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
        boolean locationChanged = false;
        if (location != null && saved.getJobStatus() == JobStatus.OPEN_FOR_BIDS) {
            safeGeoAdd(saved.getId(), location.getLatitude(), location.getLongitude());
            locationChanged = true;
        }
        eventPublisher.publishEvent(new JobModifiedEvent(jobId, clientId, JobModifiedEvent.Type.UPDATED, locationChanged));
        return jobMapper.toDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_JOB_DETAIL, key = "#jobId")
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
        // Validate sort fields against whitelist
        SortValidator.validate(filterRequest.getSortBy(), filterRequest.getSortDirection(), SortValidator.JOB_FIELDS);
        if (filterRequest.getSecondarySortBy() != null) {
            SortValidator.validate(filterRequest.getSecondarySortBy(), filterRequest.getSecondarySortDirection(), SortValidator.JOB_FIELDS);
        }

        int page = Optional.ofNullable(filterRequest.getPage()).orElse(0);
        int size = Optional.ofNullable(filterRequest.getSize()).orElse(20);

        // Build sort — virtual fields (distance, bidCount) yield Sort.unsorted()
        Sort sort = SortValidator.buildMultiSort(
                filterRequest.getSortBy(), filterRequest.getSortDirection(),
                filterRequest.getSecondarySortBy(), filterRequest.getSecondarySortDirection(),
                SortValidator.JOB_FIELDS);

        Pageable pageable = PageRequest.of(page, size, sort);
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

        // Apply virtual field sorting (distance / popularity) in-memory
        boolean distanceSorting = Boolean.TRUE.equals(filterRequest.getSortByDistance())
                || "distance".equalsIgnoreCase(filterRequest.getSortBy());
        boolean popularitySorting = "bidcount".equalsIgnoreCase(filterRequest.getSortBy())
                || "viewcount".equalsIgnoreCase(filterRequest.getSortBy());

        if (distanceSorting || popularitySorting) {
            List<JobSummaryDTO> dtos = jobPage.getContent().stream()
                    .map(job -> {
                        JobSummaryDTO dto = jobMapper.toSummaryDto(job);
                        enrichWithDistance(dto, job, filterRequest.getDistanceLatitude(), filterRequest.getDistanceLongitude());
                        enrichWithPopularity(dto, job);
                        return dto;
                    })
                    .sorted(buildVirtualComparator(filterRequest.getSortBy(), filterRequest.getSortDirection(), distanceSorting))
                    .toList();
            return new PageImpl<>(dtos, pageable, jobPage.getTotalElements());
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
        eventPublisher.publishEvent(new JobModifiedEvent(jobId, clientId, JobModifiedEvent.Type.STATUS_CHANGED, false));
        return jobMapper.toDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_WORKER_FEED, key = "#workerId + ':' + #request.hashCode()")
    public Page<JobSummaryDTO> getWorkerFeed(Long workerId, WorkerJobFeedRequest request) {
        Worker worker = workerReadRepository.findById(workerId).orElseThrow(() -> new JobValidationException("Worker not found"));

        // Validate sort fields against whitelist
        SortValidator.validate(request.getSortBy(), request.getSortDirection(), SortValidator.JOB_FIELDS);

        double radius = Optional.ofNullable(request.getRadiusKm()).orElse(25.0);
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(20);

        Sort sort = SortValidator.buildMultiSort(
                request.getSortBy(), request.getSortDirection(),
                request.getSecondarySortBy(), request.getSecondarySortDirection(),
                SortValidator.JOB_FIELDS);

        Pageable pageable = PageRequest.of(page, size, sort);

        Double workerLat = worker.getCurrentAddress() != null ? worker.getCurrentAddress().getLatitude() : null;
        Double workerLon = worker.getCurrentAddress() != null ? worker.getCurrentAddress().getLongitude() : null;

        List<Long> geoJobIds = jobGeoService.findNearbyOpenJobs(workerLat, workerLon, radius, size * 3);

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

        // Apply virtual sorting (distance / popularity) in-memory
        boolean distanceSorting = Boolean.TRUE.equals(request.getSortByDistance())
                || "distance".equalsIgnoreCase(request.getSortBy());

        List<JobSummaryDTO> dtos = filtered.stream()
                .map(job -> {
                    JobSummaryDTO dto = jobMapper.toSummaryDto(job);
                    // Always enrich worker feed with distance when worker location is available
                    if (workerLat != null && workerLon != null) {
                        enrichWithDistance(dto, job, workerLat, workerLon);
                    }
                    enrichWithPopularity(dto, job);
                    return dto;
                })
                .toList();

        if (distanceSorting || "bidcount".equalsIgnoreCase(request.getSortBy())) {
            dtos = dtos.stream()
                    .sorted(buildVirtualComparator(request.getSortBy(), request.getSortDirection(), distanceSorting))
                    .toList();
        }

        // Use the filtered count as total elements, not the DB page total,
        // because in-memory skill filtering may have reduced the result count.
        Page<JobSummaryDTO> finalPage = new PageImpl<>(dtos, pageable, dtos.size());
        return finalPage;
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
    public void deleteJob(Long jobId, Long clientId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException("Job not found"));
        if (!Objects.equals(job.getCreatedBy().getId(), clientId)) {
            throw new AccessDeniedException("You cannot delete another client's job");
        }

        boolean wasOpen = job.getJobStatus() == JobStatus.OPEN_FOR_BIDS;
        jobRepository.delete(job);
        if (wasOpen) {
            safeGeoRemove(jobId);
        }
        eventPublisher.publishEvent(new JobModifiedEvent(jobId, clientId, JobModifiedEvent.Type.DELETED, false));
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
            safeGeoRemove(job.getId());
        } else if (!wasOpen && nowOpen && job.getJobLocation() != null) {
            safeGeoAdd(job.getId(), job.getJobLocation().getLatitude(), job.getJobLocation().getLongitude());
        }
    }

    // ── Sorting helpers ──────────────────────────────────────────
    /**
     * Enriches a DTO with the distance (in km) from the given reference point.
     */
    private void enrichWithDistance(JobSummaryDTO dto, Job job, Double refLat, Double refLon) {
        if (refLat == null || refLon == null) {
            return;
        }
        Address loc = job.getJobLocation();
        if (loc != null && loc.getLatitude() != null && loc.getLongitude() != null) {
            double km = DistanceUtils.haversine(refLat, refLon, loc.getLatitude(), loc.getLongitude());
            dto.setDistanceKm(Math.round(km * 100.0) / 100.0); // 2 decimal places
        }
    }

    /**
     * Enriches a DTO with a popularity score based on bid count.
     */
    private void enrichWithPopularity(JobSummaryDTO dto, Job job) {
        long bidCount = bidRepository.countByJobId(job.getId());
        dto.setPopularityScore(PopularityUtils.calculateJobPopularityScore((int) bidCount, null));
    }

    /**
     * Builds a comparator for in-memory (virtual field) sorting of
     * {@link JobSummaryDTO}.
     */
    private Comparator<JobSummaryDTO> buildVirtualComparator(String sortBy, SortDirection direction, boolean distanceSorting) {
        SortDirection dir = direction != null ? direction : SortDirection.DESC;
        Comparator<JobSummaryDTO> comparator;

        if (distanceSorting || "distance".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(
                    JobSummaryDTO::getDistanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        } else if ("bidcount".equalsIgnoreCase(sortBy) || "viewcount".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(
                    JobSummaryDTO::getPopularityScore,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        } else {
            // fallback: createdAt DESC
            comparator = Comparator.comparing(
                    JobSummaryDTO::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        }

        return dir == SortDirection.DESC ? comparator.reversed() : comparator;
    }

    // ── Safe Redis geo wrappers ──────────────────────────────────
    /**
     * Adds a job to the Redis geo-index. If the Redis write fails, records the
     * failure in PostgreSQL for later retry, ensuring the PostgreSQL
     * transaction is NOT rolled back.
     */
    private void safeGeoAdd(Long jobId, Double latitude, Double longitude) {
        try {
            jobGeoService.addOrUpdateOpenJob(jobId, latitude, longitude);
        } catch (Exception ex) {
            log.error("Redis geo ADD failed for jobId={}, scheduling retry: {}", jobId, ex.getMessage());
            recordFailedSync(jobId, FailedGeoSync.SyncOperation.ADD, latitude, longitude, ex.getMessage());
        }
    }

    /**
     * Removes a job from the Redis geo-index. If the Redis write fails, records
     * the failure for later retry.
     */
    private void safeGeoRemove(Long jobId) {
        try {
            jobGeoService.removeOpenJob(jobId);
        } catch (Exception ex) {
            log.error("Redis geo REMOVE failed for jobId={}, scheduling retry: {}", jobId, ex.getMessage());
            recordFailedSync(jobId, FailedGeoSync.SyncOperation.REMOVE, null, null, ex.getMessage());
        }
    }

    private void recordFailedSync(Long jobId, FailedGeoSync.SyncOperation operation,
            Double latitude, Double longitude, String error) {
        try {
            if (!failedGeoSyncRepository.existsByJobIdAndOperationAndResolvedFalse(jobId, operation)) {
                failedGeoSyncRepository.save(FailedGeoSync.builder()
                        .jobId(jobId)
                        .operation(operation)
                        .latitude(latitude)
                        .longitude(longitude)
                        .lastError(error != null && error.length() > 1000 ? error.substring(0, 1000) : error)
                        .build());
            }
        } catch (Exception ex) {
            log.error("Failed to record geo sync failure for jobId={}: {}", jobId, ex.getMessage());
        }
    }
}
