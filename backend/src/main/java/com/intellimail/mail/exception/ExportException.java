package com.intellimail.mail.exception;

/** Thrown when rendering an export (e.g. PDF) fails. */
public class ExportException extends RuntimeException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
