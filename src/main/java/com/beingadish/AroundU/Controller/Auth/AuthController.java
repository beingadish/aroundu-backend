package com.beingadish.AroundU.Controller.Auth;

import org.springframework.http.ResponseEntity;

public interface AuthController {
    ResponseEntity<LoginResponseDTO> login();
}
