package com.beingadish.AroundU.Models;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobModel {
    private Long id;
    private String title;
    private String shortDescription;
    private String longDescription;
    private PriceModel price;
    private AddressModel jobLocation;
    private JobStatus jobStatus;
    private JobUrgency jobUrgency;
    private PaymentMode paymentMode;
    private List<SkillModel> requiredSkills;
    private Long createdByUserId;
    private Long assignedToWorkerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
