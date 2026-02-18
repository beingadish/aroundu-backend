package com.beingadish.AroundU.user.dto;

import com.beingadish.AroundU.common.constants.enums.Currency;
import com.beingadish.AroundU.common.dto.AddressDTO;
import com.beingadish.AroundU.common.dto.VerificationStatusDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class UserDetailDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Currency currency;
    private AddressDTO currentAddress;
    private VerificationStatusDTO verificationStatus;
    private String profileImageUrl;
}
