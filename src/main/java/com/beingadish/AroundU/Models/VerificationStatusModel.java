package com.beingadish.AroundU.Models;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VerificationStatusModel {
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime updatedAt;
}
