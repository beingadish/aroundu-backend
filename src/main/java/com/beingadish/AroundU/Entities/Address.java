package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Constants.Enums.Country;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Country country;

    @Column(nullable = false)
    @NotBlank
    private String postalCode;

    @Column
    private String city;

    @Column
    private String area;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    @Size(max = 500)
    private String fullAddress;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonBackReference
    private Client client;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return addressId != null && java.util.Objects.equals(addressId, address.addressId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
