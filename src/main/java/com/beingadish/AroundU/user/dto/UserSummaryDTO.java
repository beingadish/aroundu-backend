package com.beingadish.AroundU.user.dto;

import com.beingadish.AroundU.common.constants.enums.Currency;
import lombok.Data;

@Data
public class UserSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Currency currency;
}
