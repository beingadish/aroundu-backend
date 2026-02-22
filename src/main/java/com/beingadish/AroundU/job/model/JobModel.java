package com.beingadish.AroundU.job.model;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.model.AddressModel;
import com.beingadish.AroundU.common.model.PriceModel;
import com.beingadish.AroundU.common.model.SkillModel;
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
