package com.beingadish.AroundU.DTO.Job;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobCodeVerifyRequest {
    @NotBlank
    private String code;
}
