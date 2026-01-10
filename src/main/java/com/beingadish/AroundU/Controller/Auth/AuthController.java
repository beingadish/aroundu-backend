package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.LoginResponseDTO;
import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.beingadish.AroundU.Constants.URIConstants.AUTH_BASE;
import static com.beingadish.AroundU.Constants.URIConstants.LOGIN;

@RestController
@RequestMapping(AUTH_BASE)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerRepository;
    private final AdminRepository adminRepository;

    @PostMapping(LOGIN)
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String role = authentication.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("USER");
        Long userId = resolveUserId(loginRequest.getEmail(), role);
        String jwt = tokenProvider.generateToken(userId, loginRequest.getEmail());

        log.info("User authenticated email={} role={}", loginRequest.getEmail(), role);

        return ResponseEntity.ok(new LoginResponseDTO(jwt, "Bearer", loginRequest.getEmail(), role));
    }

    private Long resolveUserId(String email, String role) {
        return switch (role) {
            case "ROLE_CLIENT" -> clientReadRepository.findByEmail(email).map(Client::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Client not found for email: " + email));
            case "ROLE_WORKER" -> workerRepository.findByEmail(email).map(Worker::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Worker not found for email: " + email));
            case "ROLE_ADMIN" -> adminRepository.findByEmail(email).map(Admin::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Admin not found for email: " + email));
            default -> clientReadRepository.findByEmail(email).map(Client::getId)
                    .or(() -> workerRepository.findByEmail(email).map(Worker::getId))
                    .or(() -> adminRepository.findByEmail(email).map(Admin::getId))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + email));
        };
    }
}