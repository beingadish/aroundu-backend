package com.beingadish.AroundU.DTO.Payment;

import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import com.beingadish.AroundU.Constants.Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    private Long id;
    private Long jobId;
    private Long clientId;
    private Long workerId;
    private Double amount;
    private PaymentMode paymentMode;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
