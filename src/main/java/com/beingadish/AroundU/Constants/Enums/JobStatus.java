package com.beingadish.AroundU.Constants.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum JobStatus {
    CREATED("Job created"),
    OPEN_FOR_BIDS("Job open for bids"),
    BID_SELECTED_AWAITING_HANDSHAKE("Bid selected; awaiting worker handshake"),
    READY_TO_START("Worker accepted; ready to start"),
    IN_PROGRESS("Job is in progress"),
    COMPLETED("Job is finished"),
    CANCELLED("Job is cancelled");

    private final String description;
}
