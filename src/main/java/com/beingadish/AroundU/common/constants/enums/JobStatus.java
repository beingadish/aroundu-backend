package com.beingadish.AroundU.common.constants.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobStatus {
    CREATED("Job created"),
    OPEN_FOR_BIDS("Job open for bids"),
    BID_SELECTED_AWAITING_HANDSHAKE("Bid selected; awaiting worker handshake"),
    READY_TO_START("Worker accepted; ready to start"),
    IN_PROGRESS("Job is in progress"),
    COMPLETED("Job is finished"),
    CANCELLED("Job is cancelled"),
    JOB_CLOSED_DUE_TO_EXPIRATION("Job closed due to expiration");

    private final String description;
}
