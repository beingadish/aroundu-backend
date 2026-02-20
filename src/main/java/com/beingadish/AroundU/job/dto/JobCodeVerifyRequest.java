package com.beingadish.AroundU.job.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobCodeVerifyRequest {
    @NotBlank
    private String code;
}
