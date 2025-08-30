package com.beingadish.AroundU.DTO.Client;

import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ClientSignupRequestDTO {
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
    private List<AddressDTO> savedAddresses; // optional on signup
}
