package com.beingadish.AroundU.user.dto;

import com.beingadish.AroundU.common.dto.SkillDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Public-facing worker profile with only non-sensitive fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicWorkerProfileDTO {
    private Long id;
    private String name;
    private String profileImageUrl;
    private Double overallRating;
    private Integer experienceYears;
    private String certifications;
    private Boolean isOnDuty;
    private List<SkillDTO> skills;
}
