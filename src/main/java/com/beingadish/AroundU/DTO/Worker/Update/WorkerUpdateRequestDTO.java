package com.beingadish.AroundU.DTO.Worker.Update;

import com.beingadish.AroundU.Constants.Enums.Currency;
import lombok.Data;

@Data
public class WorkerUpdateRequestDTO {
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private Integer experienceYears;
    private String certifications;
    private Boolean isOnDuty;
    private String payoutAccount;
    private Currency currency;
}
