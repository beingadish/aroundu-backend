package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Constants.Enums.BidStatus;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;
import com.beingadish.AroundU.Entities.Bid;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Mappers.Bid.BidMapper;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Service.BidService;
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

    @Override
    public BidResponseDTO placeBid(Long jobId, Long workerId, BidCreateRequest request) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new EntityNotFoundException("Worker not found"));
        if (job.getJobStatus() != JobStatus.OPEN_FOR_BIDS) {
            throw new IllegalStateException("Job is not open for bids");
        }
        if (!Boolean.TRUE.equals(worker.getIsOnDuty())) {
            throw new IllegalStateException("Worker is not on duty");
        }
        Bid bid = bidMapper.toEntity(request, job, worker);
        Bid saved = bidRepository.save(bid);
        return bidMapper.toDto(saved);
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
        bidRepository.findByJob(job).stream().filter(b -> !b.getId().equals(bidId)).forEach(other -> {
            other.setStatus(BidStatus.REJECTED);
            bidRepository.save(other);
        });
        job.setJobStatus(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE);
        jobRepository.save(job);
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
