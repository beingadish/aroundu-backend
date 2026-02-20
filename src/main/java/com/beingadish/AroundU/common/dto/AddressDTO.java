package com.beingadish.AroundU.common.dto;

import com.beingadish.AroundU.common.constants.enums.Country;
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
    private String city;
    private String area;
    private Double latitude;
    private Double longitude;
    @Size(max = 500)
    private String fullAddress;
}
