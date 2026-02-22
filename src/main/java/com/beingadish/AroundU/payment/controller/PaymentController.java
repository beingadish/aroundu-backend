package com.beingadish.AroundU.payment.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.dto.PaymentResponseDTO;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.payment.service.PaymentService;
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
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.common.constants.URIConstants.JOB_BASE;

@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Escrow payment lock and release operations")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTransactionMapper paymentTransactionMapper;

    @PostMapping("/{jobId}/payments/lock")
    @Operation(summary = "Lock escrow payment", description = "Client locks the escrow amount for a job before work begins")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Escrow locked",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Escrow already locked for this job")
    })
    public ResponseEntity<PaymentResponseDTO> lock(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId,
            @Valid @RequestBody PaymentLockRequest request) {
        return new ResponseEntity<>(paymentTransactionMapper.toDto(paymentService.lockEscrow(jobId, clientId, request)), HttpStatus.CREATED);
    }

    @PostMapping("/{jobId}/payments/release")
    @Operation(summary = "Release escrow payment", description = "Client releases escrowed funds to the worker after job completion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment released",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid release code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job or payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Payment not in locked state")
    })
    public ResponseEntity<PaymentResponseDTO> release(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId,
            @Valid @RequestBody PaymentReleaseRequest request) {
        return ResponseEntity.ok(paymentTransactionMapper.toDto(paymentService.releaseEscrow(jobId, clientId, request)));
    }
}
