package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobStatusUpdateRequest {
    @NotNull
    private JobStatus newStatus;
}
