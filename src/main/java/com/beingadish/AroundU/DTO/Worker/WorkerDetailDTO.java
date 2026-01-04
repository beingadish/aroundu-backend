package com.beingadish.AroundU.DTO.Worker;

import com.beingadish.AroundU.DTO.Common.SkillDTO;
import com.beingadish.AroundU.DTO.User.UserDetailDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class WorkerDetailDTO extends UserDetailDTO {
    private List<SkillDTO> skills;
    private List<Long> engagedJobIds;
}
