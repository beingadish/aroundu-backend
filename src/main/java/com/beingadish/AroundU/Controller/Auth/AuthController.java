package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.DTO.LoginResponseDTO;
import org.springframework.http.ResponseEntity;

public interface AuthController {
    ResponseEntity<LoginResponseDTO> login();
}
