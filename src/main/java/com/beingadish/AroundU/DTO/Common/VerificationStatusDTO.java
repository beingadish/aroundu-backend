package com.beingadish.AroundU.DTO.Common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VerificationStatusDTO {
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime updatedAt;
}
