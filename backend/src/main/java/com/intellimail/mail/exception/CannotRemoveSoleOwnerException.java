package com.intellimail.mail.exception;

/** Thrown when trying to remove the only remaining OWNER of an organization. */
public class CannotRemoveSoleOwnerException extends RuntimeException {

    public CannotRemoveSoleOwnerException(String message) {
        super(message);
    }
}
