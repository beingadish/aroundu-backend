package com.beingadish.AroundU.review.service;

import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;

import java.util.List;

public interface ReviewService {

    /**
     * Submit a review for a completed job. Only the job owner (client) can review the worker.
     * One review per job per client.
     */
    ReviewResponseDTO submitReview(Long jobId, Long clientId, ReviewCreateRequest request);

    /**
     * Submit a review for a completed job. Only the assigned worker can review the client.
     * One review per job per worker.
     */
    ReviewResponseDTO submitWorkerReview(Long jobId, Long workerId, ReviewCreateRequest request);

    /**
     * Check if a review has been submitted for a job by a specific user.
     */
    boolean hasReviewedJob(Long jobId, Long reviewerId);

    /**
     * Get all reviews for a specific worker.
     */
    List<ReviewResponseDTO> getWorkerReviews(Long workerId);

    /**
     * Get the review for a specific job, if one exists.
     */
    ReviewResponseDTO getReviewForJob(Long jobId);
}
