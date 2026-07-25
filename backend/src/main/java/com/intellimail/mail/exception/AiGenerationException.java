package com.intellimail.mail.exception;

/** Thrown when the Azure OpenAI call fails after retries are exhausted (Module 6's client/retry layer). */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
