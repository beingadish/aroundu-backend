package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Constants.Enums.Country;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Size(max = 500)
    private String fullAddress;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
