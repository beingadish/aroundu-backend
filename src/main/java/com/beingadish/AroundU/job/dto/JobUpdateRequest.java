package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.dto.PriceDTO;
import lombok.Data;

import java.util.List;

@Data
public class JobUpdateRequest {
    private String title;
    private String shortDescription;
    private String longDescription;
    private PriceDTO price;
    private Long jobLocationId;
    private JobUrgency jobUrgency;
    private JobStatus jobStatus;
    private List<Long> requiredSkillIds;
    private PaymentMode paymentMode;
}
