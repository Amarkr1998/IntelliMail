package com.intellimail.mail.prompt;

import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptCatalogTest {

    private final SystemPromptCatalog catalog = new SystemPromptCatalog();

    @ParameterizedTest
    @EnumSource(RequestType.class)
    void everyRequestType_hasANonBlankSystemPrompt(RequestType requestType) {
        String prompt = catalog.systemPromptFor(requestType);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("IntelliMail");
    }

    @Test
    void professionalRewrite_mentionsProfessionalTone() {
        assertThat(catalog.systemPromptFor(RequestType.PROFESSIONAL_REWRITE)).containsIgnoringCase("professional");
    }

    @Test
    void translate_mentionsTranslation() {
        assertThat(catalog.systemPromptFor(RequestType.TRANSLATE)).containsIgnoringCase("translate");
    }

    @Test
    void subjectLine_instructsReturningOnlyTheSubject() {
        assertThat(catalog.systemPromptFor(RequestType.SUBJECT_LINE)).containsIgnoringCase("subject line");
    }

    @Test
    void distinctRequestTypes_produceDistinctPrompts() {
        String professional = catalog.systemPromptFor(RequestType.PROFESSIONAL_REWRITE);
        String casual = catalog.systemPromptFor(RequestType.CASUAL_REWRITE);

        assertThat(professional).isNotEqualTo(casual);
    }
}
