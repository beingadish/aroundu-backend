package com.beingadish.AroundU.user.model;

import com.beingadish.AroundU.common.constants.enums.Currency;
import lombok.Data;
import com.beingadish.AroundU.common.model.AddressModel;
import com.beingadish.AroundU.common.model.VerificationStatusModel;

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
    private String profileImageUrl;
}
