package com.beingadish.AroundU.DTO.Common;

import com.beingadish.AroundU.Constants.Enums.Country;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressDTO {
    private Long id;
    @NotNull
    private Country country;
    @NotBlank
    private String postalCode;
    @Size(max = 500)
    private String fullAddress;
}
