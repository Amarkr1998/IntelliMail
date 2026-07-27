package com.intellimail.mail.exception;

/** Thrown when a user who already belongs to an organization tries to create or join another. */
public class UserAlreadyInOrganizationException extends RuntimeException {

    public UserAlreadyInOrganizationException(String message) {
        super(message);
    }
}
