package com.intellimail.mail.exception;

/** Thrown when confirm/reject is called on an agent task that has no pending action. */
public class AgentTaskNotAwaitingConfirmationException extends RuntimeException {

    public AgentTaskNotAwaitingConfirmationException(String message) {
        super(message);
    }
}
