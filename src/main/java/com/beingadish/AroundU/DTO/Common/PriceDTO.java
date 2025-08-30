package com.beingadish.AroundU.DTO.Common;

import com.beingadish.AroundU.Constants.Enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PriceDTO {
    @NotNull
    private Currency currency;
    @NotNull
    @Positive
    private Double amount;
}
