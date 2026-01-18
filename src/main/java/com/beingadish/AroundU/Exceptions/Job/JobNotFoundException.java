package com.beingadish.AroundU.Exceptions.Job;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String message) {
        super(message);
    }
}
