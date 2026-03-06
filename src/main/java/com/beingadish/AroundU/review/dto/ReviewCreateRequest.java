package com.beingadish.AroundU.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewCreateRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Double rating;

    @Size(max = 1200, message = "Review comment must not exceed 1200 characters")
    private String reviewComment;
}
