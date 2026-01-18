package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobStatusUpdateRequest {
    @NotNull
    private JobStatus newStatus;
}
