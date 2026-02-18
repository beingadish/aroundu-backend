package com.beingadish.AroundU.user.dto.worker;

import com.beingadish.AroundU.common.dto.SkillDTO;
import com.beingadish.AroundU.user.dto.UserDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDetailDTO extends UserDetailDTO {
    private List<SkillDTO> skills;
    private List<Long> engagedJobIds;
    private Integer experienceYears;
    private String certifications;
    private Boolean isOnDuty;
    private String payoutAccount;
}
