package com.beingadish.AroundU.Constants.Enums;

import lombok.Getter;

@Getter
public enum JobStatus {
    CREATED("Job is created "),
    DEAL_OPEN("Job deal in open"),
    DEAL_CLOSED_AND_ASSIGNED("Job deal is closed & assigned"),
    IN_PROGRESS("Job is in progress"),
    COMPLETED("Job is finished"),
    CANCELLED("Job is cancelled");

    private final String description;

    // Constructor for the enum
    JobStatus(String description) {
        this.description = description;
    }

    // Getter method for the description
    public String getDescription() {
        return description;
    }
}
