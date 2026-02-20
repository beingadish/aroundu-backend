package com.beingadish.AroundU.bid.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BidHandshakeRequest {
    @NotNull
    private Boolean accepted;
}
