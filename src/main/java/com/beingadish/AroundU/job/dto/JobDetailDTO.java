package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.dto.AddressDTO;
import com.beingadish.AroundU.common.dto.PriceDTO;
import com.beingadish.AroundU.common.dto.SkillDTO;
import com.beingadish.AroundU.user.dto.UserSummaryDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDetailDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private String longDescription;
    private PriceDTO price;
    private AddressDTO jobLocation;
    private JobStatus jobStatus;
    private JobUrgency jobUrgency;
    private PaymentMode paymentMode;
    private List<SkillDTO> requiredSkills;
    private UserSummaryDTO createdBy;
    private WorkerBriefDTO assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
