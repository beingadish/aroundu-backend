package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.Auth.LoginResponseDTO;
import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Security.JwtTokenProvider;
import com.beingadish.AroundU.Service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerRepository;
    private final AdminRepository adminRepository;

    @Override
    public LoginResponseDTO authenticate(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");
        Long userId = resolveUserId(loginRequest.getEmail(), role);
        String jwt = tokenProvider.generateToken(userId, loginRequest.getEmail(), role);

        log.info("User authenticated email={} role={} id={}", loginRequest.getEmail(), role, userId);

        return new LoginResponseDTO(userId, jwt, "Bearer", loginRequest.getEmail(), role);
    }

    private Long resolveUserId(String email, String role) {
        return switch (role) {
            case "ROLE_CLIENT" ->
                clientReadRepository.findByEmail(email).map(Client::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Client not found for email: " + email));
            case "ROLE_WORKER" ->
                workerRepository.findByEmail(email).map(Worker::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Worker not found for email: " + email));
            case "ROLE_ADMIN" ->
                adminRepository.findByEmail(email).map(Admin::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found for email: " + email));
            default ->
                clientReadRepository.findByEmail(email).map(Client::getId)
                .or(() -> workerRepository.findByEmail(email).map(Worker::getId))
                .or(() -> adminRepository.findByEmail(email).map(Admin::getId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + email));
        };
    }
}
