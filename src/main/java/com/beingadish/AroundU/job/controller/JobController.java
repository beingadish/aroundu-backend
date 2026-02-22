package com.beingadish.AroundU.job.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.job.dto.JobCreateRequest;
import com.beingadish.AroundU.job.dto.JobDetailDTO;
import com.beingadish.AroundU.job.dto.JobFilterRequest;
import com.beingadish.AroundU.job.dto.JobStatusUpdateRequest;
import com.beingadish.AroundU.job.dto.JobSummaryDTO;
import com.beingadish.AroundU.job.dto.JobUpdateRequest;
import com.beingadish.AroundU.job.dto.WorkerJobFeedRequest;
import com.beingadish.AroundU.job.service.JobService;
import com.beingadish.AroundU.common.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.beingadish.AroundU.infrastructure.ratelimit.RateLimit;

import static com.beingadish.AroundU.common.constants.URIConstants.JOB_BASE;

@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting, filtering, and worker feed")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create job", description = "Client creates a job with required skills and location. Only CLIENT role permitted.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Job created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Only clients can create jobs")
    })
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and #clientId == authentication.principal.id)")
    @RateLimit(capacity = 5, refillTokens = 5, refillMinutes = 60)
    public ResponseEntity<ApiResponse<JobDetailDTO>> createJob(@RequestParam Long clientId, @Valid @RequestBody JobCreateRequest request) {
        JobDetailDTO dto = jobService.createJob(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PatchMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Update job details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobDetailDTO>> updateJob(@PathVariable Long jobId, @RequestParam Long clientId, @RequestBody JobUpdateRequest request) {
        JobDetailDTO dto = jobService.updateJob(jobId, clientId, request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{jobId}/status")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Update job status", description = "Allows valid forward-only transitions", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobDetailDTO>> updateJobStatus(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody JobStatusUpdateRequest request) {
        JobDetailDTO dto = jobService.updateJobStatus(jobId, clientId, request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{jobId}/worker-status")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    @Operation(summary = "Worker updates job status", description = "Worker can start task or mark complete", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the assigned worker")
    })
    public ResponseEntity<ApiResponse<JobDetailDTO>> updateJobStatusByWorker(@PathVariable Long jobId, @RequestParam Long workerId, @Valid @RequestBody JobStatusUpdateRequest request) {
        JobDetailDTO dto = jobService.updateJobStatusByWorker(jobId, workerId, request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{jobId}/worker-cancel")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('WORKER') and #workerId == authentication.principal.id)")
    @Operation(summary = "Worker cancels job",
            description = "Worker cancels an accepted/in-progress job. Triggers cancellation penalty.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job cancelled, penalty applied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state for cancellation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the assigned worker")
    })
    public ResponseEntity<ApiResponse<JobDetailDTO>> cancelJobByWorker(
            @PathVariable Long jobId, @RequestParam Long workerId) {
        JobDetailDTO dto = jobService.cancelJobByWorker(jobId, workerId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Delete job", description = "Client deletes a job they created", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<String>> deleteJob(@PathVariable Long jobId, @RequestParam Long clientId) {
        jobService.deleteJob(jobId, clientId);
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully"));
    }

    @GetMapping("/client/{clientId}/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Get client job detail", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobDetailDTO>> getJobForClient(@PathVariable Long jobId, @PathVariable Long clientId) {
        JobDetailDTO dto = jobService.getJobForClient(jobId, clientId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "List client jobs", description = "Filter by statuses and date range", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<JobSummaryDTO>>> getClientJobs(@PathVariable Long clientId, @Valid @ModelAttribute JobFilterRequest filter) {
        PageResponse<JobSummaryDTO> page = jobService.getClientJobs(clientId, filter);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/client/{clientId}/past")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "List past jobs", description = "Completed or cancelled jobs", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<JobSummaryDTO>>> getClientPastJobs(@PathVariable Long clientId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        PageResponse<JobSummaryDTO> result = jobService.getClientPastJobs(clientId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/worker/{workerId}/feed")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    @Operation(summary = "Worker feed", description = "Open jobs filtered by skills and proximity", security = @SecurityRequirement(name = "bearerAuth"))
    @RateLimit(capacity = 30, refillTokens = 30, refillMinutes = 1)
    public ResponseEntity<ApiResponse<PageResponse<JobSummaryDTO>>> getWorkerFeed(@PathVariable Long workerId, @Valid @ModelAttribute WorkerJobFeedRequest request) {
        PageResponse<JobSummaryDTO> page = jobService.getWorkerFeed(workerId, request);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/worker/{workerId}/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    @Operation(summary = "Worker job detail", description = "Visible if open or assigned", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobDetailDTO>> getJobForWorker(@PathVariable Long workerId, @PathVariable Long jobId) {
        JobDetailDTO dto = jobService.getJobForWorker(jobId, workerId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Public job detail", description = "Basic job detail (authorization may apply)")
    public ResponseEntity<ApiResponse<JobDetailDTO>> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobDetail(jobId)));
    }

    @GetMapping
    @Operation(summary = "Search jobs by location")
    public ResponseEntity<ApiResponse<List<JobSummaryDTO>>> listJobs(@RequestParam(required = false) String city, @RequestParam(required = false) String area, @RequestParam(required = false) List<Long> skillIds) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listJobs(city, area, skillIds)));
    }
}
