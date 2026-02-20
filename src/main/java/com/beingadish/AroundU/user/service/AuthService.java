package com.beingadish.AroundU.user.service;

import com.beingadish.AroundU.user.dto.auth.LoginRequestDTO;
import com.beingadish.AroundU.user.dto.auth.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO authenticate(LoginRequestDTO loginRequest);
}
