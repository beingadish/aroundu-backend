package com.beingadish.AroundU.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;
}
