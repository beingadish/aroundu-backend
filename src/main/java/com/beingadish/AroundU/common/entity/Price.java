package com.beingadish.AroundU.common.entity;

import com.beingadish.AroundU.common.constants.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Price {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Positive
    @Column(nullable = false)
    private Double amount;
}
