package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import com.beingadish.AroundU.DTO.Common.PriceDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class JobCreateRequest {
    @NotBlank
    private String title;
    @Size(max = 200)
    private String shortDescription;
    @NotBlank
    private String longDescription;
    @NotNull
    private PriceDTO price;
    @NotNull
    private Long jobLocationId;
    @NotNull
    private JobUrgency jobUrgency;
    @NotNull
    private List<Long> requiredSkillIds;
    @NotNull
    private PaymentMode paymentMode;
}
