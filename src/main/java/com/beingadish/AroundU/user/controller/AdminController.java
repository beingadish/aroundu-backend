package com.beingadish.AroundU.user.controller;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.beingadish.AroundU.common.constants.URIConstants.ADMIN_BASE;

/**
 * Admin-only endpoints for platform overview and management. All endpoints
 * require ROLE_ADMIN.
 */
@RestController
@RequestMapping(ADMIN_BASE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard and management endpoints")
public class AdminController {

    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;

    @GetMapping("/overview")
    @Operation(summary = "Platform overview",
            description = "Returns key platform statistics: total users, active jobs, today's activity.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClients", clientRepository.count());
        stats.put("totalWorkers", workerRepository.count());
        stats.put("activeJobs", jobRepository.findByJobStatus(JobStatus.IN_PROGRESS).size());
        stats.put("openJobs", jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS).size());
        stats.put("jobsCreatedToday", jobRepository.countByCreatedAtBetween(todayStart, todayEnd));
        stats.put("jobsCompletedToday", jobRepository.countByJobStatusAndCreatedAtBetween(
                JobStatus.COMPLETED, todayStart, todayEnd));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
