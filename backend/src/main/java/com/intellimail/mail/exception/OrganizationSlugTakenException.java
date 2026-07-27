package com.intellimail.mail.exception;

public class OrganizationSlugTakenException extends RuntimeException {

    public OrganizationSlugTakenException(String slug) {
        super("An organization with slug '" + slug + "' already exists");
    }
}
