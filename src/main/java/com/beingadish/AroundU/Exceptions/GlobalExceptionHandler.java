package com.beingadish.AroundU.Exceptions;

import com.beingadish.AroundU.DTO.Client.ClientResponseDTO;
import com.beingadish.AroundU.Exceptions.Client.ClientAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Client.ClientNotFoundException;
import com.beingadish.AroundU.Exceptions.Client.ClientValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ClientAlreadyExistException.class)
    public ResponseEntity<ClientResponseDTO> handleClientAlreadyExists(ClientAlreadyExistException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ClientResponseDTO.forError("Error: " + ex.getMessage()));
    }

    @ExceptionHandler(ClientValidationException.class)
    public ResponseEntity<ClientResponseDTO> handleValidationException(ClientValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ClientResponseDTO.forError("Validation failed: " + ex.getMessage()));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ClientResponseDTO> handleClientNotFoundException(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ClientResponseDTO.forError("Not Found: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClientResponseDTO> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ClientResponseDTO.forError("Internal Server Error: " + ex.getMessage()));
    }
}
