package com.intellimail.mail.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Restricts a {@code RequestType} field to the rewrite/grammar/expand/shorten styles used by POST /api/email/improve. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RewriteStyleValidator.class)
@Documented
public @interface ValidRewriteStyle {

    String message() default "Style must be one of: PROFESSIONAL_REWRITE, FRIENDLY_REWRITE, FORMAL_REWRITE, "
            + "CASUAL_REWRITE, GRAMMAR_CORRECTION, EXPAND, SHORTEN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
