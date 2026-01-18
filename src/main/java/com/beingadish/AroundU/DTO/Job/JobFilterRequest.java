package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class JobFilterRequest {
    private List<JobStatus> statuses;
    private LocalDate startDate;
    private LocalDate endDate;
    @Min(0)
    private Integer page = 0;
    @Positive
    @Max(100)
    private Integer size = 20;
}
