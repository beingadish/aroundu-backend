package com.beingadish.AroundU.common.model;

import com.beingadish.AroundU.common.constants.enums.Currency;
import lombok.Data;

@Data
public class PriceModel {
    private Currency currency;
    private Double amount;
}
