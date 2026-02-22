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
    COMPLETED_PENDING_PAYMENT("Worker marked complete; awaiting client payment release"),
    PAYMENT_RELEASED("Client released payment; job fully settled"),
    COMPLETED("Job is finished"),
    CANCELLED("Job is cancelled"),
    JOB_CLOSED_DUE_TO_EXPIRATION("Job closed due to expiration");

    private final String description;

    /**
     * Returns true if this status represents a terminal (non-modifiable) state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == JOB_CLOSED_DUE_TO_EXPIRATION || this == PAYMENT_RELEASED;
    }

    /**
     * Returns true if reviews are allowed for this job status.
     */
    public boolean isReviewEligible() {
        return this == PAYMENT_RELEASED || this == COMPLETED;
    }
}
