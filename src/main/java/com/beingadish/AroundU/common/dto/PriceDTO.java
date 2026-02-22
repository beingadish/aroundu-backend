package com.beingadish.AroundU.common.dto;

import com.beingadish.AroundU.common.constants.enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDTO {
    @NotNull
    private Currency currency;
    @NotNull
    @Positive
    private Double amount;
}
