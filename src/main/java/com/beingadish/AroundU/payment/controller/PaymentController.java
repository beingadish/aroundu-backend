package com.beingadish.AroundU.payment.controller;

import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.dto.PaymentResponseDTO;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.common.constants.URIConstants.JOB_BASE;

@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTransactionMapper paymentTransactionMapper;

    @PostMapping("/{jobId}/payments/lock")
    public ResponseEntity<PaymentResponseDTO> lock(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody PaymentLockRequest request) {
        return new ResponseEntity<>(paymentTransactionMapper.toDto(paymentService.lockEscrow(jobId, clientId, request)), HttpStatus.CREATED);
    }

    @PostMapping("/{jobId}/payments/release")
    public ResponseEntity<PaymentResponseDTO> release(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody PaymentReleaseRequest request) {
        return ResponseEntity.ok(paymentTransactionMapper.toDto(paymentService.releaseEscrow(jobId, clientId, request)));
    }
}
