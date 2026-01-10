package com.beingadish.AroundU.Exceptions;

import com.beingadish.AroundU.DTO.Common.ApiResponse;
import com.beingadish.AroundU.Exceptions.Client.ClientAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Client.ClientNotFoundException;
import com.beingadish.AroundU.Exceptions.Client.ClientValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientAlreadyExistException.class)
    public ResponseEntity<ApiResponse<?>> handleClientAlreadyExists(ClientAlreadyExistException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage(), 409));
    }

    @ExceptionHandler(ClientValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ClientValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage(), 400));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleClientNotFoundException(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage(), 404));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied: " + ex.getMessage(), 403));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication failed: " + ex.getMessage(), 401));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Internal server error: " + ex.getMessage(), 500));
    }
}