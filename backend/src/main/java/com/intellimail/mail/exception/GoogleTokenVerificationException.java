package com.intellimail.mail.exception;

/** Thrown when a Google Sign-In ID token fails signature, audience, issuer, or email-verified checks. */
public class GoogleTokenVerificationException extends RuntimeException {

    public GoogleTokenVerificationException(String message) {
        super(message);
    }

    public GoogleTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
