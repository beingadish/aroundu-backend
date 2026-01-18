package com.beingadish.AroundU.Exceptions.Worker;

public class WorkerAlreadyExistException extends RuntimeException {
    public WorkerAlreadyExistException(String message) {
        super(message);
    }
}
