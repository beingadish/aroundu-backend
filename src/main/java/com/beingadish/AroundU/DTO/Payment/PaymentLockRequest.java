package com.beingadish.AroundU.DTO.Payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentLockRequest {
    @NotNull
    @Positive
    private Double amount;
}
