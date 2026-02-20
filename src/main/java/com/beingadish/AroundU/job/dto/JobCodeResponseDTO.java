package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for job confirmation codes. Never exposes both codes
 * simultaneously — only the code relevant to the current step.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCodeResponseDTO {

    private Long id;
    private Long jobId;
    private JobCodeStatus status;
    private String startCode;
    private String releaseCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
