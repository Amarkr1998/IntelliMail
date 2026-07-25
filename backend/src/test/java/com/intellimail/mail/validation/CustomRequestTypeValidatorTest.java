package com.intellimail.mail.validation;

import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomRequestTypeValidatorTest {

    private final CustomRequestTypeValidator validator = new CustomRequestTypeValidator();

    @Test
    void acceptsEachAllowedGeneratorType() {
        assertThat(validator.isValid(RequestType.MEETING_REQUEST, null)).isTrue();
        assertThat(validator.isValid(RequestType.THANK_YOU, null)).isTrue();
        assertThat(validator.isValid(RequestType.APOLOGY, null)).isTrue();
        assertThat(validator.isValid(RequestType.SALES, null)).isTrue();
        assertThat(validator.isValid(RequestType.HR, null)).isTrue();
        assertThat(validator.isValid(RequestType.MARKETING, null)).isTrue();
        assertThat(validator.isValid(RequestType.COLD_OUTREACH, null)).isTrue();
        assertThat(validator.isValid(RequestType.CUSTOM_PROMPT, null)).isTrue();
    }

    @Test
    void rejectsRequestTypesHandledByOtherEndpoints() {
        assertThat(validator.isValid(RequestType.GENERATE_REPLY, null)).isFalse();
        assertThat(validator.isValid(RequestType.PROFESSIONAL_REWRITE, null)).isFalse();
        assertThat(validator.isValid(RequestType.TRANSLATE, null)).isFalse();
    }
}
