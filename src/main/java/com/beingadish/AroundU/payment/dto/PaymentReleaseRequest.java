package com.beingadish.AroundU.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentReleaseRequest {
    @NotBlank
    private String releaseCode;
}
