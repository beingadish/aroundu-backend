package com.beingadish.AroundU.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-facing client profile with only non-sensitive fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicClientProfileDTO {
    private Long id;
    private String name;
    private String profileImageUrl;
}
