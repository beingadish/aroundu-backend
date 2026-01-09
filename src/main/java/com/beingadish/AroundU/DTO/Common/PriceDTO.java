package com.beingadish.AroundU.DTO.Common;

import com.beingadish.AroundU.Constants.Enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceDTO {
    @NotNull
    private Currency currency;
    @NotNull
    @Positive
    private Double amount;
}
