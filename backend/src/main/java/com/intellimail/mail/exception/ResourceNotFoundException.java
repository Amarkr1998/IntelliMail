package com.intellimail.mail.exception;

/** Generic 404 for any entity looked up by id (templates, email requests, replies, ...). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with id: " + identifier);
    }
}
