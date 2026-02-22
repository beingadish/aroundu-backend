package com.beingadish.AroundU.review.service;

import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.exception.JobNotFoundException;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;
import com.beingadish.AroundU.review.entity.Review;
import com.beingadish.AroundU.review.exception.ReviewNotFoundException;
import com.beingadish.AroundU.review.exception.ReviewValidationException;
import com.beingadish.AroundU.review.mapper.ReviewMapper;
import com.beingadish.AroundU.review.repository.ReviewRepository;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.exception.ClientNotFoundException;
import com.beingadish.AroundU.user.exception.WorkerNotFoundException;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final JobRepository jobRepository;
    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerReadRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(Long jobId, Long clientId, ReviewCreateRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        // Only review-eligible jobs can be reviewed (PAYMENT_RELEASED or COMPLETED)
        if (!job.getJobStatus().isReviewEligible()) {
            throw new ReviewValidationException("Can only review jobs with status PAYMENT_RELEASED or COMPLETED");
        }

        // Only the job owner can review
        if (!job.getCreatedBy().getId().equals(clientId)) {
            throw new ReviewValidationException("Only the job owner can submit a review");
        }

        // Job must have an assigned worker
        Worker assignedWorker = job.getAssignedTo();
        if (assignedWorker == null) {
            throw new ReviewValidationException("Job has no assigned worker to review");
        }

        // Prevent duplicate reviews
        if (reviewRepository.existsByJobIdAndReviewerId(jobId, clientId)) {
            throw new ReviewValidationException("A review has already been submitted for this job");
        }

        Client reviewer = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        Review review = reviewMapper.toEntity(request, job, assignedWorker, reviewer);
        Review saved = reviewRepository.save(review);

        // Update worker average rating
        updateWorkerRating(assignedWorker.getId());

        log.info("Review submitted for job {} by client {} for worker {}", jobId, clientId, assignedWorker.getId());
        return reviewMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getWorkerReviews(Long workerId) {
        if (workerReadRepository.findById(workerId).isEmpty()) {
            throw new WorkerNotFoundException("Worker not found with id: " + workerId);
        }
        List<Review> reviews = reviewRepository.findByWorkerId(workerId);
        return reviewMapper.toDtoList(reviews);
    }

    @Override
    @Transactional
    public ReviewResponseDTO submitWorkerReview(Long jobId, Long workerId, ReviewCreateRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (!job.getJobStatus().isReviewEligible()) {
            throw new ReviewValidationException("Can only review jobs with status PAYMENT_RELEASED or COMPLETED");
        }

        Worker worker = job.getAssignedTo();
        if (worker == null || !worker.getId().equals(workerId)) {
            throw new ReviewValidationException("Only the assigned worker can submit a review");
        }

        // Use workerId as "reviewerId" for the duplicate check
        if (reviewRepository.existsByJobIdAndReviewerId(jobId, workerId)) {
            throw new ReviewValidationException("A review has already been submitted for this job by this worker");
        }

        // Worker reviews the client — we store with reviewer=null (no Client entity for worker),
        // but we keep the same Review entity shape. The worker field stays the same for aggregation.
        Client reviewedClient = job.getCreatedBy();

        Review review = Review.builder()
                .rating(request.getRating())
                .reviewComment(request.getReviewComment())
                .job(job)
                .worker(worker) // Self-reference for the worker's review record
                .reviewer(reviewedClient) // The client being reviewed
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Worker review submitted for job {} by worker {} for client {}", jobId, workerId, reviewedClient.getId());
        return reviewMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReviewedJob(Long jobId, Long reviewerId) {
        return reviewRepository.existsByJobIdAndReviewerId(jobId, reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewForJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        Long reviewerId = job.getCreatedBy().getId();
        Review review = reviewRepository.findByJobIdAndReviewerId(jobId, reviewerId)
                .orElseThrow(() -> new ReviewNotFoundException("No review found for job: " + jobId));

        return reviewMapper.toDto(review);
    }

    private void updateWorkerRating(Long workerId) {
        Optional<Double> avgRating = reviewRepository.averageRatingByWorkerId(workerId);
        avgRating.ifPresent(rating -> {
            workerReadRepository.findById(workerId).ifPresent(worker -> {
                worker.setOverallRating(Math.round(rating * 100.0) / 100.0);
                // Worker entity is managed; rating will be flushed at tx commit
            });
        });
    }
}
