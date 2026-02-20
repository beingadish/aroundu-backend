package com.beingadish.AroundU.infrastructure.config;

import com.beingadish.AroundU.common.constants.enums.Country;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.user.entity.Admin;
import com.beingadish.AroundU.user.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.name:Admin}")
    private String adminName;

    @Bean
    CommandLineRunner seedAdmin() {
        return args -> {
            adminRepository.findByEmail(adminEmail).ifPresentOrElse(
                    a -> {}, // already exists, skip
                    () -> {
                        Admin admin = Admin.builder()
                                .name(adminName)
                                .email(adminEmail)
                                .phoneNumber("+10000000000") // placeholder; adjust validation
                                .hashedPassword(passwordEncoder.encode(adminPassword))
                                .currentAddress(Address.builder()
                                        .country(Country.IN)
                                        .postalCode("560103")
                                        .city("Bangalore")
                                        .area("Bellandur")
                                        .fullAddress("Green Glen Layout, KVR Layout, Behind Cloud Nine Hospital, Bellandur Main Road, Bellandur, Bangalore, Karnataka, India")
                                        .build())
                                .build();
                        adminRepository.save(admin);
                    }
            );
        };
    }
}