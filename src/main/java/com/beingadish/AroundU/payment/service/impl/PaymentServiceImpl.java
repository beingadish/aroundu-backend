package com.beingadish.AroundU.payment.service.impl;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import com.beingadish.AroundU.payment.service.PaymentService;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("paymentServiceImpl")
@RequiredArgsConstructor
@Transactional
@Slf4j
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

            if (!job.getCreatedBy().getId().equals(clientId)) {
                throw new IllegalStateException("Client does not own this job");
            }
            if (job.getAssignedTo() == null) {
                throw new IllegalStateException("Cannot lock payment before worker assignment");
            }

            // Prevent duplicate escrow locks
            if (paymentTransactionRepository.findByJob(job).isPresent()) {
                throw new IllegalStateException("Escrow payment already exists for this job");
            }

            // Validate job is in an appropriate state for escrow lock
            if (job.getJobStatus() != JobStatus.READY_TO_START &&
                    job.getJobStatus() != JobStatus.BID_SELECTED_AWAITING_HANDSHAKE) {
                throw new IllegalStateException("Escrow can only be locked when job is READY_TO_START or BID_SELECTED_AWAITING_HANDSHAKE");
            }

            Worker worker = job.getAssignedTo();
            PaymentTransaction tx = paymentTransactionMapper.toEntity(request, job, client, worker);
            PaymentTransaction saved = paymentTransactionRepository.save(tx);
            metricsService.getEscrowLockedCounter().increment();
            log.info("Escrow locked for job={} client={} amount={}", jobId, clientId, request.getAmount());
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

            // Job must be in COMPLETED_PENDING_PAYMENT or IN_PROGRESS (for legacy flow)
            if (job.getJobStatus() != JobStatus.COMPLETED_PENDING_PAYMENT &&
                    job.getJobStatus() != JobStatus.IN_PROGRESS &&
                    job.getJobStatus() != JobStatus.COMPLETED) {
                throw new IllegalStateException("Cannot release payment in current job status: " + job.getJobStatus());
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

            // Transition job to PAYMENT_RELEASED
            job.setJobStatus(JobStatus.PAYMENT_RELEASED);
            jobRepository.save(job);

            metricsService.getEscrowReleasedCounter().increment();
            log.info("Escrow released for job={} client={}", jobId, clientId);
            return saved;
        });
    }
}
