package com.beingadish.AroundU.DTO.Worker.Update;

import com.beingadish.AroundU.Constants.Enums.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkerUpdateRequestDTO {

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Email(message = "Must be a valid email address")
    private String email;

    @Size(min = 7, max = 15, message = "Phone number must be between 7 and 15 characters")
    private String phoneNumber;

    private String profileImageUrl;

    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears;

    @Size(max = 500, message = "Certifications must not exceed 500 characters")
    private String certifications;

    private Boolean isOnDuty;
    private String payoutAccount;
    private Currency currency;
}
