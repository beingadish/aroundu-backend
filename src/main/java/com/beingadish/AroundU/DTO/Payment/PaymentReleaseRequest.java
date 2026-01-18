package com.beingadish.AroundU.DTO.Payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentReleaseRequest {
    @NotBlank
    private String releaseCode;
}
