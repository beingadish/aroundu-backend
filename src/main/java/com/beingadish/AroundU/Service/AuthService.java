package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.Auth.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO authenticate(LoginRequestDTO loginRequest);
}
