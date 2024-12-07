package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Records.SkillRecord;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@Builder
public class JobEntity {

    @Generated
    private Long jobId;

    private String jobTitle;

    private Date jobPostingTime;

    private String jobDescription;

    private List<SkillRecord> skillRequiredList;

}
