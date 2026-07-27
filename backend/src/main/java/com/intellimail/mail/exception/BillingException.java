package com.intellimail.mail.exception;

/** Wraps a checked {@code com.stripe.exception.StripeException} - callers should catch this common supertype. */
public class BillingException extends RuntimeException {

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
