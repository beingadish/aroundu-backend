package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import com.beingadish.AroundU.DTO.Common.PriceDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobSummaryDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private JobStatus jobStatus;
    private JobUrgency jobUrgency;
    private PriceDTO price;
    private PaymentMode paymentMode;
    private LocalDateTime createdAt;
}
