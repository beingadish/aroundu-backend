package com.beingadish.AroundU.DTO.Bid;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BidCreateRequest {
    @NotNull
    @Positive
    private Double bidAmount;

    private String partnerName;

    @Positive
    private Double partnerFee;

    private String notes;
}
