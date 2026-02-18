package com.beingadish.AroundU.DTO.Client.Update;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientUpdateRequestDTO {

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Email(message = "Must be a valid email address")
    private String email;

    @Size(min = 7, max = 15, message = "Phone number must be between 7 and 15 characters")
    private String phoneNumber;

    private String profileImageUrl;
}
