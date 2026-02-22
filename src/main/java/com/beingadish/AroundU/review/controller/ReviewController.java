package com.beingadish.AroundU.review.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;
import com.beingadish.AroundU.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import static com.beingadish.AroundU.common.constants.URIConstants.REVIEW_BASE;

@RestController
@RequestMapping(REVIEW_BASE)
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review submission and retrieval for completed jobs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('CLIENT') and #clientId == authentication.principal.id")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit review", description = "Client submits a review for a completed job's assigned worker")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review submitted",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Review already exists")
    })
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitReview(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponseDTO dto = reviewService.submitReview(jobId, clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PostMapping("/jobs/{jobId}/worker")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Worker submits review", description = "Worker submits a review for the client after job completion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Worker review submitted",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Review already exists")
    })
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitWorkerReview(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Worker ID", required = true) @RequestParam Long workerId,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponseDTO dto = reviewService.submitWorkerReview(jobId, workerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping("/jobs/{jobId}/eligibility")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Check review eligibility", description = "Check if a user has already reviewed a job")
    public ResponseEntity<ApiResponse<Boolean>> checkReviewEligibility(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "User ID (client or worker)", required = true) @RequestParam Long userId) {
        boolean hasReviewed = reviewService.hasReviewedJob(jobId, userId);
        return ResponseEntity.ok(ApiResponse.success(!hasReviewed));
    }

    @GetMapping("/workers/{workerId}")
    @Operation(summary = "Get worker reviews", description = "Retrieve all reviews for a worker (public)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Worker not found")
    })
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getWorkerReviews(
            @Parameter(description = "Worker ID", required = true) @PathVariable Long workerId) {
        List<ReviewResponseDTO> reviews = reviewService.getWorkerReviews(workerId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/jobs/{jobId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get job review", description = "Retrieve the review for a specific job")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No review found for job")
    })
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> getJobReview(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId) {
        ReviewResponseDTO dto = reviewService.getReviewForJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
