package com.beingadish.AroundU.bid.service.impl;

import com.beingadish.AroundU.common.constants.enums.BidStatus;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.entity.Bid;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.bid.mapper.BidMapper;
import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.bid.service.BidDuplicateCheckService;
import com.beingadish.AroundU.bid.service.BidService;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final ClientRepository clientRepository;
    private final BidMapper bidMapper;
    private final MetricsService metricsService;
    private final BidDuplicateCheckService bidDuplicateCheckService;

    @Override
    public BidResponseDTO placeBid(Long jobId, Long workerId, BidCreateRequest request) {
        return metricsService.recordTimer(metricsService.getBidPlacementTimer(), () -> {
            Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
            Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new EntityNotFoundException("Worker not found"));
            if (job.getJobStatus() != JobStatus.OPEN_FOR_BIDS) {
                throw new IllegalStateException("Job is not open for bids");
            }
            if (!Boolean.TRUE.equals(worker.getIsOnDuty())) {
                throw new IllegalStateException("Worker is not on duty");
            }
            bidDuplicateCheckService.validateNoDuplicateBid(workerId, jobId);
            Bid bid = bidMapper.toEntity(request, job, worker);
            Bid saved = bidRepository.save(bid);
            bidDuplicateCheckService.recordBid(workerId, jobId);
            metricsService.getBidsPlacedCounter().increment();
            return bidMapper.toDto(saved);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponseDTO> listBidsForJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        return bidMapper.toDtoList(bidRepository.findByJob(job));
    }

    @Override
    public BidResponseDTO acceptBid(Long bidId, Long clientId) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(() -> new EntityNotFoundException("Bid not found"));
        Job job = bid.getJob();
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found"));
        if (!job.getCreatedBy().getId().equals(client.getId())) {
            throw new IllegalStateException("Client does not own this job");
        }
        if (job.getJobStatus() != JobStatus.OPEN_FOR_BIDS) {
            throw new IllegalStateException("Job is not accepting bids");
        }
        bid.setStatus(BidStatus.SELECTED);
        bidRepository.save(bid);
        int rejectedCount = bidRepository.rejectOtherBids(job, bidId);
        job.setJobStatus(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE);
        jobRepository.save(job);
        metricsService.getBidsAcceptedCounter().increment();
        for (int i = 0; i < rejectedCount; i++) {
            metricsService.getBidsRejectedCounter().increment();
        }
        return bidMapper.toDto(bid);
    }

    @Override
    public BidResponseDTO handshake(Long bidId, Long workerId, BidHandshakeRequest request) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(() -> new EntityNotFoundException("Bid not found"));
        if (!bid.getWorker().getId().equals(workerId)) {
            throw new IllegalStateException("Worker does not own this bid");
        }
        Job job = bid.getJob();
        if (job.getJobStatus() != JobStatus.BID_SELECTED_AWAITING_HANDSHAKE) {
            throw new IllegalStateException("Handshake not allowed in current job status");
        }
        if (bid.getStatus() != BidStatus.SELECTED) {
            throw new IllegalStateException("Bid is not selected by client");
        }
        if (Boolean.TRUE.equals(request.getAccepted())) {
            bid.setStatus(BidStatus.SELECTED);
            job.setAssignedTo(bid.getWorker());
            job.setJobStatus(JobStatus.READY_TO_START);
        } else {
            bid.setStatus(BidStatus.REJECTED);
        }
        bidRepository.save(bid);
        jobRepository.save(job);
        return bidMapper.toDto(bid);
    }
}
