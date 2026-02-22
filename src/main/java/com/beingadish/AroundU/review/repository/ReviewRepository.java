package com.beingadish.AroundU.review.repository;

import com.beingadish.AroundU.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByWorkerId(Long workerId);

    Page<Review> findByWorkerId(Long workerId, Pageable pageable);

    boolean existsByJobIdAndReviewerId(Long jobId, Long reviewerId);

    Optional<Review> findByJobIdAndReviewerId(Long jobId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.worker.id = :workerId")
    Optional<Double> averageRatingByWorkerId(@Param("workerId") Long workerId);

    long countByWorkerId(Long workerId);
}
