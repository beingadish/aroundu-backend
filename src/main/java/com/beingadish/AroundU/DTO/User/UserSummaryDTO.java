package com.beingadish.AroundU.DTO.User;

import com.beingadish.AroundU.Constants.Enums.Currency;
import lombok.Data;

@Data
public class UserSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Currency currency;
}
