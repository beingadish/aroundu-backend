package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Skill;
import com.beingadish.AroundU.Mappers.Job.JobMapper;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Service.JobService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final SkillRepository skillRepository;
    private final JobMapper jobMapper;

    @Override
    public JobDetailDTO createJob(Long clientId, JobCreateRequest request) {
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found"));
        Address location = addressRepository.findById(request.getJobLocationId()).orElseThrow(() -> new EntityNotFoundException("Job location not found"));
        Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.getRequiredSkillIds()));
        Job job = jobMapper.toEntity(request, location, skills, client);
        job.setJobStatus(JobStatus.OPEN_FOR_BIDS);
        Job saved = jobRepository.save(job);
        return jobMapper.toDetailDto(saved);
    }

    @Override
    public JobDetailDTO updateJob(Long jobId, JobUpdateRequest request) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        Address location = null;
        if (request.getJobLocationId() != null) {
            location = addressRepository.findById(request.getJobLocationId()).orElseThrow(() -> new EntityNotFoundException("Job location not found"));
        }
        Set<Skill> skills = null;
        if (request.getRequiredSkillIds() != null) {
            skills = new HashSet<>(skillRepository.findAllById(request.getRequiredSkillIds()));
        }
        jobMapper.updateEntity(request, job, location, skills);
        Job saved = jobRepository.save(job);
        return jobMapper.toDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailDTO getJobDetail(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
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
}
