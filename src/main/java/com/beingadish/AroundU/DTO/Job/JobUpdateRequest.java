package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import com.beingadish.AroundU.DTO.Common.PriceDTO;
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
