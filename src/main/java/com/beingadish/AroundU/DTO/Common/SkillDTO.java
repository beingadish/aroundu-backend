package com.beingadish.AroundU.DTO.Common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillDTO {
    private Long id;
    private String skillName;
}
