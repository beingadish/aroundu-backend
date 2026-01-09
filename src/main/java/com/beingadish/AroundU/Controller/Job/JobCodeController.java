package com.beingadish.AroundU.Controller.Job;

import com.beingadish.AroundU.DTO.Job.JobCodeVerifyRequest;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import com.beingadish.AroundU.Service.JobCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes endpoints for generating and validating job start/release codes to protect execution and payout.
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobCodeController {

    private final JobCodeService jobCodeService;

    /**
     * Client generates start and release codes after selecting a bid.
     */
    @PostMapping("/{jobId}/codes")
    public ResponseEntity<JobConfirmationCode> generate(@PathVariable Long jobId, @RequestParam Long clientId) {
        return ResponseEntity.ok(jobCodeService.generateCodes(jobId, clientId));
    }

    /**
     * Assigned worker confirms the start code before beginning work.
     */
    @PostMapping("/{jobId}/codes/start")
    public ResponseEntity<JobConfirmationCode> verifyStart(@PathVariable Long jobId,
                                                           @RequestParam Long workerId,
                                                           @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(jobCodeService.verifyStartCode(jobId, workerId, request.getCode()));
    }

    /**
     * Client confirms the release code to mark the job complete and trigger payout.
     */
    @PostMapping("/{jobId}/codes/release")
    public ResponseEntity<JobConfirmationCode> verifyRelease(@PathVariable Long jobId,
                                                             @RequestParam Long clientId,
                                                             @Valid @RequestBody JobCodeVerifyRequest request) {
        return ResponseEntity.ok(jobCodeService.verifyReleaseCode(jobId, clientId, request.getCode()));
    }
}
