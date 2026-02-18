package com.beingadish.AroundU.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private Long userId;
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;
}
