package com.beingadish.AroundU.bid.dto;

import com.beingadish.AroundU.common.constants.enums.BidStatus;
import lombok.Data;

@Data
public class BidResponseDTO {
    private Long id;
    private Long jobId;
    private Long workerId;
    private Double bidAmount;
    private String partnerName;
    private Double partnerFee;
    private String notes;
    private BidStatus status;
}
