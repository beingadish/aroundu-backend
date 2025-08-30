package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.Common.PriceDTO;
import com.beingadish.AroundU.DTO.Common.SkillDTO;
import com.beingadish.AroundU.DTO.User.UserSummaryDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDetailDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private String longDescription;
    private PriceDTO price;
    private AddressDTO jobLocation;
    private JobStatus jobStatus;
    private JobUrgency jobUrgency;
    private List<SkillDTO> requiredSkills;
    private UserSummaryDTO createdBy;
    private WorkerBriefDTO assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
