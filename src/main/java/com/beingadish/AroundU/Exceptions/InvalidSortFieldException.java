package com.beingadish.AroundU.Exceptions;

/**
 * Thrown when a client-supplied sort field name is not in the whitelist for the
 * requested endpoint.
 */
public class InvalidSortFieldException extends RuntimeException {

    public InvalidSortFieldException(String field, String endpoint) {
        super("Invalid sort field '%s' for %s endpoint".formatted(field, endpoint));
    }

    public InvalidSortFieldException(String message) {
        super(message);
    }
}
