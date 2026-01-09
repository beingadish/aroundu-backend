package com.beingadish.AroundU.DTO.Bid;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BidHandshakeRequest {
    @NotNull
    private Boolean accepted;
}
