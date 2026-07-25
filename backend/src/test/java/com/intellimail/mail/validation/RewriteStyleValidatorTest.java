package com.intellimail.mail.validation;

import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewriteStyleValidatorTest {

    private final RewriteStyleValidator validator = new RewriteStyleValidator();

    @Test
    void acceptsEachAllowedRewriteStyle() {
        assertThat(validator.isValid(RequestType.PROFESSIONAL_REWRITE, null)).isTrue();
        assertThat(validator.isValid(RequestType.FRIENDLY_REWRITE, null)).isTrue();
        assertThat(validator.isValid(RequestType.FORMAL_REWRITE, null)).isTrue();
        assertThat(validator.isValid(RequestType.CASUAL_REWRITE, null)).isTrue();
        assertThat(validator.isValid(RequestType.GRAMMAR_CORRECTION, null)).isTrue();
        assertThat(validator.isValid(RequestType.EXPAND, null)).isTrue();
        assertThat(validator.isValid(RequestType.SHORTEN, null)).isTrue();
    }

    @Test
    void rejectsRequestTypesOutsideRewriteFamily() {
        assertThat(validator.isValid(RequestType.GENERATE_REPLY, null)).isFalse();
        assertThat(validator.isValid(RequestType.SALES, null)).isFalse();
        assertThat(validator.isValid(RequestType.CUSTOM_PROMPT, null)).isFalse();
    }

    @Test
    void treatsNullAsValid_leavingItToNotNullToReport() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
