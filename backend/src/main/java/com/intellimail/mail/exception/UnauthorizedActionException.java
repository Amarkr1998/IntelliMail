package com.intellimail.mail.exception;

/** Thrown when an authenticated user attempts to act on a resource they do not own (e.g. another user's template). */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
