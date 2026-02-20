package com.beingadish.AroundU.payment.service;

import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;

public interface PaymentService {
    PaymentTransaction lockEscrow(Long jobId, Long clientId, PaymentLockRequest request);

    PaymentTransaction releaseEscrow(Long jobId, Long clientId, PaymentReleaseRequest request);
}
