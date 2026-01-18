package com.beingadish.AroundU.DTO.User;

import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.Common.VerificationStatusDTO;
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
