package com.intellimail.mail.exception;

/** Thrown when an action requiring organization membership is attempted by a solo (org-less) user. */
public class UserNotInOrganizationException extends RuntimeException {

    public UserNotInOrganizationException(String message) {
        super(message);
    }
}
