package com.beingadish.AroundU.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponseDTO {
    private Long id;
    private Long jobId;
    private Long workerId;
    private Long reviewerId;
    private String reviewerName;
    private Double rating;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
