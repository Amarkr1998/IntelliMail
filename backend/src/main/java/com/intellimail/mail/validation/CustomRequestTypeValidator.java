package com.intellimail.mail.validation;

import com.intellimail.mail.enums.RequestType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.EnumSet;
import java.util.Set;

public class CustomRequestTypeValidator implements ConstraintValidator<ValidCustomRequestType, RequestType> {

    private static final Set<RequestType> ALLOWED = EnumSet.of(
            RequestType.MEETING_REQUEST,
            RequestType.THANK_YOU,
            RequestType.APOLOGY,
            RequestType.SALES,
            RequestType.HR,
            RequestType.MARKETING,
            RequestType.COLD_OUTREACH,
            RequestType.CUSTOM_PROMPT
    );

    @Override
    public boolean isValid(RequestType value, ConstraintValidatorContext context) {
        return value == null || ALLOWED.contains(value);
    }
}
