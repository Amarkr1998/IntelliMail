package com.intellimail.mail.dto.email;

import com.intellimail.mail.enums.RequestType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirms the {@code @ValidRewriteStyle} / {@code @ValidCustomRequestType}
 * annotations are actually wired to their validators end-to-end (as opposed
 * to unit-testing the validator classes in isolation), using the real
 * Bean Validation runtime rather than a mocked ConstraintValidatorContext.
 */
class EmailRequestValidationTest {

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void emailImproveRequest_withValidRewriteStyle_hasNoViolations() {
        EmailImproveRequest request = new EmailImproveRequest("Some content", RequestType.PROFESSIONAL_REWRITE, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void emailImproveRequest_withOutOfFamilyStyle_isRejected() {
        EmailImproveRequest request = new EmailImproveRequest("Some content", RequestType.SALES, null);

        Set<ConstraintViolation<EmailImproveRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("style"));
    }

    @Test
    void emailCustomRequest_withValidGeneratorType_hasNoViolations() {
        EmailCustomRequest request = new EmailCustomRequest(RequestType.COLD_OUTREACH, "Reach out about our product", null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void emailCustomRequest_withRequestTypeBelongingToAnotherEndpoint_isRejected() {
        EmailCustomRequest request = new EmailCustomRequest(RequestType.TRANSLATE, "Some context", null, null, null);

        Set<ConstraintViolation<EmailCustomRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestType"));
    }

    @Test
    void emailGenerateRequest_withReferenceContextOverLimit_isRejected() {
        EmailGenerateRequest request = new EmailGenerateRequest("Some content", null, null, "A".repeat(20_001));

        Set<ConstraintViolation<EmailGenerateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("referenceContext"));
    }

    @Test
    void emailGenerateRequest_withoutReferenceContext_hasNoViolations() {
        EmailGenerateRequest request = new EmailGenerateRequest("Some content", null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
