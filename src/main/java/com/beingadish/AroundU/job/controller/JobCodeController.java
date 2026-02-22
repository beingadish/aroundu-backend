package com.beingadish.AroundU.job.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.job.dto.JobCodeResponseDTO;
import com.beingadish.AroundU.job.dto.JobCodeVerifyRequest;
import com.beingadish.AroundU.job.mapper.JobConfirmationCodeMapper;
import com.beingadish.AroundU.job.service.JobCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.common.constants.URIConstants.JOB_BASE;

/**
 * Exposes endpoints for generating and validating job start/release codes to
 * protect execution and payout.
 */
@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
@Tag(name = "Job Codes", description = "Job start/release code generation and verification")
@SecurityRequirement(name = "bearerAuth")
public class JobCodeController {

    private final JobCodeService jobCodeService;
    private final JobConfirmationCodeMapper codeMapper;

    @PostMapping("/{jobId}/codes")
    @Operation(summary = "Generate job codes", description = "Client generates start and release codes after selecting a bid. Only the start code is returned initially.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Codes generated",
                content = @Content(schema = @Schema(implementation = JobCodeResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the job owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Codes already generated")
    })
    public ResponseEntity<JobCodeResponseDTO> generate(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId) {
        return ResponseEntity.ok(codeMapper.toDtoWithStartCodeOnly(jobCodeService.generateCodes(jobId, clientId)));
    }

    @PostMapping("/{jobId}/codes/start")
    @Operation(summary = "Verify start code", description = "Assigned worker confirms the start code before beginning work. The status update confirms verification.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Start code verified",
                content = @Content(schema = @Schema(implementation = JobCodeResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job or code not found")
    })
    public ResponseEntity<JobCodeResponseDTO> verifyStart(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Worker ID (assigned worker)", required = true) @RequestParam Long workerId,
            @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(codeMapper.toDtoWithoutCodes(jobCodeService.verifyStartCode(jobId, workerId, request.getCode())));
    }

    @PostMapping("/{jobId}/codes/release")
    @Operation(summary = "Verify release code", description = "Client confirms the release code to mark the job complete and trigger payout.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Release code verified, job completed",
                content = @Content(schema = @Schema(implementation = JobCodeResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job or code not found")
    })
    public ResponseEntity<JobCodeResponseDTO> verifyRelease(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId,
            @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(codeMapper.toDtoWithoutCodes(jobCodeService.verifyReleaseCode(jobId, clientId, request.getCode())));
    }

    @PostMapping("/{jobId}/otp/regenerate")
    @Operation(summary = "Regenerate OTP codes",
            description = "Client regenerates start/release OTPs, invalidating previous codes. Rate-limited to once per minute.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP regenerated",
                content = @Content(schema = @Schema(implementation = JobCodeResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Rate limited or invalid state")
    })
    public ResponseEntity<JobCodeResponseDTO> regenerate(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId) {
        return ResponseEntity.ok(codeMapper.toDtoWithStartCodeOnly(jobCodeService.regenerateCodes(jobId, clientId)));
    }
}
