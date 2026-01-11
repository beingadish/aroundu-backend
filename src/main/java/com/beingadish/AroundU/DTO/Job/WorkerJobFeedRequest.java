package com.beingadish.AroundU.DTO.Job;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class WorkerJobFeedRequest {
    private List<Long> skillIds;
    private Double radiusKm;
    @Min(0)
    private Integer page = 0;
    @Positive
    @Max(100)
    private Integer size = 20;
}
