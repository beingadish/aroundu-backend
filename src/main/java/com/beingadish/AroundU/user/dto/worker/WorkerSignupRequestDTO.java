package com.beingadish.AroundU.user.dto.worker;

import com.beingadish.AroundU.common.constants.enums.Currency;
import com.beingadish.AroundU.common.dto.AddressDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkerSignupRequestDTO {
    @NotBlank
    private String name;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String password;
    @NotNull
    private Currency currency;
    @Valid
    @NotNull
    private AddressDTO currentAddress;

    private List<String> skillIds;
}