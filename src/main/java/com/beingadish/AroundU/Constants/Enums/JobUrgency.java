package com.beingadish.AroundU.Constants.Enums;

public enum JobUrgency {
    SUPER_URGENT(100), URGENT(50), MEDIUM(25), NORMAL(0);

    private final Integer urgencyPrice;

    JobUrgency(Integer urgencyPrice) {
        this.urgencyPrice = urgencyPrice;
    }

    Integer getUrgencyPrice() {
        return urgencyPrice;
    }
}