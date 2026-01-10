package com.beingadish.AroundU.Controller.Payment;

import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.DTO.Payment.PaymentReleaseRequest;
import com.beingadish.AroundU.Entities.PaymentTransaction;
import com.beingadish.AroundU.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.Constants.URIConstants.JOB_BASE;

@RestController
@RequestMapping(JOB_BASE)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{jobId}/payments/lock")
    public ResponseEntity<PaymentTransaction> lock(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody PaymentLockRequest request) {
        return new ResponseEntity<>(paymentService.lockEscrow(jobId, clientId, request), HttpStatus.CREATED);
    }

    @PostMapping("/{jobId}/payments/release")
    public ResponseEntity<PaymentTransaction> release(@PathVariable Long jobId, @RequestParam Long clientId, @Valid @RequestBody PaymentReleaseRequest request) {
        return ResponseEntity.ok(paymentService.releaseEscrow(jobId, clientId, request));
    }
}
