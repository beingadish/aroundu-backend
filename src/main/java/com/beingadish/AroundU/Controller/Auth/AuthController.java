package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.Auth.LoginResponseDTO;
import com.beingadish.AroundU.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beingadish.AroundU.RateLimit.RateLimit;

import static com.beingadish.AroundU.Constants.URIConstants.AUTH_BASE;
import static com.beingadish.AroundU.Constants.URIConstants.LOGIN;

@RestController
@RequestMapping(AUTH_BASE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping(LOGIN)
    @RateLimit(capacity = 5, refillTokens = 5, refillMinutes = 15)
    @Operation(summary = "Authenticate user", description = "Authenticate by email/password and receive JWT", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authenticated", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return ResponseEntity.ok(authService.authenticate(loginRequest));
    }
}
