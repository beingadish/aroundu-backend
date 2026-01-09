package com.beingadish.AroundU.DTO.Client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {
    private boolean success;
    private String message;

    public static ClientResponseDTO forError(String message) {
        return new ClientResponseDTO(false, message);
    }

    public static ClientResponseDTO forSuccess(String message) {
        return new ClientResponseDTO(true, message);
    }
}
