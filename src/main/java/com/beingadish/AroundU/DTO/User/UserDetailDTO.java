package com.beingadish.AroundU.DTO.User;

import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.Common.VerificationStatusDTO;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class UserDetailDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Currency currency;
    private AddressDTO currentAddress;
    private VerificationStatusDTO verificationStatus;
}
