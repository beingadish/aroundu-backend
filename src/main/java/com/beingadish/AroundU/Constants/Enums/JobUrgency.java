package com.beingadish.AroundU.Constants.Enums;

public enum JobUrgency {
    URGENT(50),
    MEDIUM(20),
    NORMAL(0);

    private Integer urgencyPrice;

    JobUrgency(Integer urgencyPrice) {
        this.urgencyPrice = urgencyPrice;
    }

    Integer getUrgencyPrice() {
        return urgencyPrice;
    }
}
