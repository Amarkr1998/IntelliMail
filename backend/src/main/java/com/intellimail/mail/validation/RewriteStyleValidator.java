package com.intellimail.mail.validation;

import com.intellimail.mail.enums.RequestType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.EnumSet;
import java.util.Set;

public class RewriteStyleValidator implements ConstraintValidator<ValidRewriteStyle, RequestType> {

    private static final Set<RequestType> ALLOWED = EnumSet.of(
            RequestType.PROFESSIONAL_REWRITE,
            RequestType.FRIENDLY_REWRITE,
            RequestType.FORMAL_REWRITE,
            RequestType.CASUAL_REWRITE,
            RequestType.GRAMMAR_CORRECTION,
            RequestType.EXPAND,
            RequestType.SHORTEN
    );

    @Override
    public boolean isValid(RequestType value, ConstraintValidatorContext context) {
        // null is left for @NotNull to report; this validator only checks membership.
        return value == null || ALLOWED.contains(value);
    }
}
