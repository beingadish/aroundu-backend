package com.beingadish.AroundU.DTO.Bid;

import com.beingadish.AroundU.Constants.Enums.BidStatus;
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
