package com.beingadish.AroundU.Exceptions.Worker;

public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(String message) {
        super(message);
    }
}
