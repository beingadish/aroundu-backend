package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.dto.PriceDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    /**
     * Distance in km from the reference point. Only populated when distance
     * sorting is active.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double distanceKm;

    /**
     * Popularity score (bid count / views). Only populated when popularity
     * sorting is active.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer popularityScore;
}
