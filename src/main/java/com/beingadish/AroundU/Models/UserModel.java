package com.beingadish.AroundU.Models;

import com.beingadish.AroundU.Constants.Enums.Currency;
import lombok.Data;

@Data
public class UserModel {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Currency currency;
    private AddressModel currentAddress;
    private VerificationStatusModel verificationStatus;
    private String hashedPassword;
}
