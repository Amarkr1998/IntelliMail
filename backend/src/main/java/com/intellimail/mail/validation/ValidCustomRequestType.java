package com.intellimail.mail.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Restricts a {@code RequestType} field to the "compose from scratch" generators used by POST /api/email/custom. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CustomRequestTypeValidator.class)
@Documented
public @interface ValidCustomRequestType {

    String message() default "Request type must be one of: MEETING_REQUEST, THANK_YOU, APOLOGY, SALES, HR, "
            + "MARKETING, COLD_OUTREACH, CUSTOM_PROMPT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
