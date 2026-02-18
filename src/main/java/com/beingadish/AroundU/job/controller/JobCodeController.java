package com.beingadish.AroundU.job.controller;

import com.beingadish.AroundU.job.dto.JobCodeResponseDTO;
import com.beingadish.AroundU.job.dto.JobCodeVerifyRequest;
import com.beingadish.AroundU.job.mapper.JobConfirmationCodeMapper;
import com.beingadish.AroundU.job.service.JobCodeService;
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
public class JobCodeController {

    private final JobCodeService jobCodeService;
    private final JobConfirmationCodeMapper codeMapper;

    /**
     * Client generates start and release codes after selecting a bid. Only the
     * start code is returned — the release code is held back until the release
     * step.
     */
    @PostMapping("/{jobId}/codes")
    public ResponseEntity<JobCodeResponseDTO> generate(@PathVariable Long jobId, @RequestParam Long clientId) {
        return ResponseEntity.ok(codeMapper.toDtoWithStartCodeOnly(jobCodeService.generateCodes(jobId, clientId)));
    }

    /**
     * Assigned worker confirms the start code before beginning work. Codes are
     * hidden from the response — only the status update matters.
     */
    @PostMapping("/{jobId}/codes/start")
    public ResponseEntity<JobCodeResponseDTO> verifyStart(@PathVariable Long jobId, @RequestParam Long workerId, @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(codeMapper.toDtoWithoutCodes(jobCodeService.verifyStartCode(jobId, workerId, request.getCode())));
    }

    /**
     * Client confirms the release code to mark the job complete and trigger
     * payout. Codes are hidden from the response.
     */
    @PostMapping("/{jobId}/codes/release")
    public ResponseEntity<JobCodeResponseDTO> verifyRelease(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(codeMapper.toDtoWithoutCodes(jobCodeService.verifyReleaseCode(jobId, clientId, request.getCode())));
    }
}
