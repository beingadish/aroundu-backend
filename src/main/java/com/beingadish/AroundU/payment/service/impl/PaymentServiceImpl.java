package com.beingadish.AroundU.payment.service.impl;

import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.payment.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("paymentServiceImpl")
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final JobConfirmationCodeRepository jobConfirmationCodeRepository;
    private final MetricsService metricsService;

    @Override
    public PaymentTransaction lockEscrow(Long jobId, Long clientId, PaymentLockRequest request) {
        return metricsService.recordTimer(metricsService.getEscrowLockTimer(), () -> {
            Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
            Client client = clientRepository.findById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found"));
            if (job.getAssignedTo() == null) {
                throw new IllegalStateException("Cannot lock payment before worker assignment");
            }
            Worker worker = job.getAssignedTo();
            PaymentTransaction tx = paymentTransactionMapper.toEntity(request, job, client, worker);
            PaymentTransaction saved = paymentTransactionRepository.save(tx);
            metricsService.getEscrowLockedCounter().increment();
            return saved;
        });
    }

    @Override
    public PaymentTransaction releaseEscrow(Long jobId, Long clientId, PaymentReleaseRequest request) {
        return metricsService.recordTimer(metricsService.getEscrowReleaseTimer(), () -> {
            Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
            if (!job.getCreatedBy().getId().equals(clientId)) {
                throw new IllegalStateException("Client does not own this job");
            }
            var codes = jobConfirmationCodeRepository.findByJob(job).orElseThrow(() -> new EntityNotFoundException("Confirmation codes not found"));
            if (!codes.getReleaseCode().equals(request.getReleaseCode())) {
                throw new IllegalArgumentException("Invalid release code");
            }
            PaymentTransaction tx = paymentTransactionRepository.findByJob(job).orElseThrow(() -> new EntityNotFoundException("Payment transaction not found"));
            if (tx.getStatus() != PaymentStatus.ESCROW_LOCKED) {
                throw new IllegalStateException("Payment is not locked in escrow");
            }
            tx.setStatus(PaymentStatus.RELEASED);
            PaymentTransaction saved = paymentTransactionRepository.save(tx);
            metricsService.getEscrowReleasedCounter().increment();
            return saved;
        });
    }
}
