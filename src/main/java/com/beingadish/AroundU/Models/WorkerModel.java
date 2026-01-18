package com.beingadish.AroundU.Models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class WorkerModel extends UserModel {
    private List<Long> engagedJobIds;
    private Integer experienceYears;
    private String certifications;
    private Boolean isOnDuty;
    private String payoutAccount;
}
