package com.beingadish.AroundU.DTO.Client;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientRequestDTO {
    private Long clientId;
    private String clientName;

    @NonNull
    private String clientEmail;
    @NonNull
    private String password;
}
