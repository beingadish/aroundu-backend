package com.beingadish.AroundU.Constants.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum JobStatus {
    CREATED("<< Job Created >>"),
    DEAL_OPEN("<< Job is Open for Deal >>"),
    DEAL_CLOSED_AND_ASSIGNED("<< Job Closed & Assigned >>"),
    IN_PROGRESS("<< Job is in Progress >>"),
    COMPLETED("<< Job is Finished >>"),
    CANCELLED("<< Job is Cancelled >>");

    private final String description;
}
