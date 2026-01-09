package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.DTO.Payment.PaymentReleaseRequest;
import com.beingadish.AroundU.Entities.PaymentTransaction;

public interface PaymentService {
    PaymentTransaction lockEscrow(Long jobId, Long clientId, PaymentLockRequest request);

    PaymentTransaction releaseEscrow(Long jobId, Long clientId, PaymentReleaseRequest request);
}
