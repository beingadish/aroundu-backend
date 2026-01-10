package com.beingadish.AroundU.Controller.Job;

import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.Service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.beingadish.AroundU.Constants.URIConstants.JOB_BASE;

@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobDetailDTO> createJob(@RequestParam Long clientId, @Valid @RequestBody JobCreateRequest request) {
        return new ResponseEntity<>(jobService.createJob(clientId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/{jobId}")
    public ResponseEntity<JobDetailDTO> updateJob(@PathVariable Long jobId, @RequestBody JobUpdateRequest request) {
        return ResponseEntity.ok(jobService.updateJob(jobId, request));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobDetailDTO> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.getJobDetail(jobId));
    }

    @GetMapping
    public ResponseEntity<List<JobSummaryDTO>> listJobs(@RequestParam(required = false) String city, @RequestParam(required = false) String area, @RequestParam(required = false) List<Long> skillIds) {
        return ResponseEntity.ok(jobService.listJobs(city, area, skillIds));
    }
}
