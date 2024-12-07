package com.beingadish.AroundU.DTO.Client;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientResponseDTO {
    @NonNull
    private String message;

    // Optional fields, included only when necessary
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private String password;

    // Static factory methods for different use cases
    public static ClientResponseDTO forError(String message) {
        return ClientResponseDTO.builder()
                .message(message)
                .build();
    }

    public static ClientResponseDTO forClientInfo(String message, Long clientId, String clientName, String clientEmail, String password) {
        return ClientResponseDTO.builder()
                .message(message)
                .clientId(clientId)
                .clientName(clientName)
                .clientEmail(clientEmail)
                .password(password)
                .build();
    }
}