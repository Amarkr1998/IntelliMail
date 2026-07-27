package com.intellimail.mail.exception;

/** Thrown when a portal-session (or similar) is requested before the organization has ever checked out. */
public class NoBillingAccountException extends RuntimeException {

    public NoBillingAccountException(String message) {
        super(message);
    }
}
