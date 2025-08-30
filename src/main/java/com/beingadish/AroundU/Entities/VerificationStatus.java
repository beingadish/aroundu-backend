package com.beingadish.AroundU.Entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatus {

    private Boolean isVerified;

    private LocalDateTime verifiedAt;

    private LocalDateTime expiryDate;

    private LocalDateTime updatedAt;


}
