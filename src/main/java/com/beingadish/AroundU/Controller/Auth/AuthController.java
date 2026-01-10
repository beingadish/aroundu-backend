package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.LoginResponseDTO;
import com.beingadish.AroundU.Security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.beingadish.AroundU.Constants.URIConstants.*;

@RestController
@RequestMapping(AUTH_BASE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping(LOGIN)
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        String role = authentication.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("USER");

        return ResponseEntity.ok(new LoginResponseDTO(jwt, "Bearer", loginRequest.getEmail(), role));
    }
}