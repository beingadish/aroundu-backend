package com.beingadish.AroundU.DTO.Client.Update;

import lombok.Data;

@Data
public class ClientUpdateRequestDTO {
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
}
