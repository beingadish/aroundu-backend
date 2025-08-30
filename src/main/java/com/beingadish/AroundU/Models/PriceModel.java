package com.beingadish.AroundU.Models;

import com.beingadish.AroundU.Constants.Enums.Currency;
import lombok.Data;

@Data
public class PriceModel {
    private Currency currency;
    private Double amount;
}
