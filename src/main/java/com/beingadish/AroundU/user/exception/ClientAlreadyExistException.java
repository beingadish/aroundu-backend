package com.beingadish.AroundU.user.exception;

public class ClientAlreadyExistException extends RuntimeException {
    public ClientAlreadyExistException(String message) {
        super(message);
    }
}
