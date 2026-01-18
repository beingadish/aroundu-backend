package com.beingadish.AroundU.Models;

import com.beingadish.AroundU.Constants.Enums.Country;
import lombok.Data;

@Data
public class AddressModel {
    private Long id;
    private Country country;
    private String postalCode;
    private String city;
    private String area;
    private Double latitude;
    private Double longitude;
    private String fullAddress;
    private Long userId; // optional backlink
}
