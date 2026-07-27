package com.intellimail.mail.prompt;

import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptFactoryTest {

    private PromptFactory promptFactory;

    @BeforeEach
    void setUp() {
        promptFactory = new PromptFactory(new SystemPromptCatalog());
    }

    @Test
    void forGenerateReply_withoutInstructions_omitsInstructionsBlock() {
        PreparedPrompt prompt = promptFactory.forGenerateReply("Can we meet Tuesday?", null, null);

        assertThat(prompt.userPrompt()).contains("Can we meet Tuesday?");
        assertThat(prompt.userPrompt()).doesNotContain("Additional instructions");
    }

    @Test
    void forGenerateReply_withInstructions_includesThem() {
        PreparedPrompt prompt = promptFactory.forGenerateReply("Can we meet Tuesday?", "Keep it under 3 sentences", null);

        assertThat(prompt.userPrompt()).contains("Additional instructions from the user: Keep it under 3 sentences");
    }

    @Test
    void forGenerateReply_withOverrideSystemPrompt_usesOverrideInsteadOfCatalog() {
        PreparedPrompt prompt = promptFactory.forGenerateReply("Hi", null, "Custom persona: reply as a pirate.");

        assertThat(prompt.systemPrompt()).isEqualTo("Custom persona: reply as a pirate.");
    }

    @Test
    void forRewrite_usesCatalogSystemPrompt_whenNoOverrideGiven() {
        PreparedPrompt prompt = promptFactory.forRewrite(RequestType.CASUAL_REWRITE, "Dear Sir, ...", null);

        assertThat(prompt.systemPrompt()).containsIgnoringCase("casual");
        assertThat(prompt.userPrompt()).contains("Dear Sir, ...");
    }

    @Test
    void forTranslate_rendersContentAndTargetLanguage() {
        PreparedPrompt prompt = promptFactory.forTranslate("Hello there", "French");

        assertThat(prompt.userPrompt()).contains("Target language: French");
        assertThat(prompt.userPrompt()).contains("Hello there");
    }

    @Test
    void forCustomGenerator_withoutCustomPrompt_omitsInstructionsBlock() {
        PreparedPrompt prompt = promptFactory.forCustomGenerator(RequestType.THANK_YOU, "Thanking a vendor for fast delivery", null, null);

        assertThat(prompt.userPrompt()).contains("Thanking a vendor for fast delivery");
        assertThat(prompt.userPrompt()).doesNotContain("Additional instructions");
    }

    @Test
    void forCustomGenerator_withCustomPrompt_includesIt() {
        PreparedPrompt prompt = promptFactory.forCustomGenerator(
                RequestType.COLD_OUTREACH, "Reaching out to a potential client", "Mention our 20% launch discount", null);

        assertThat(prompt.userPrompt()).contains("Mention our 20% launch discount");
    }

    @Test
    void forVoiceCommand_withoutLanguage_omitsLanguageInstruction() {
        PreparedPrompt prompt = promptFactory.forVoiceCommand("Draft a reply saying I'll be there", null);

        assertThat(prompt.userPrompt()).contains("Draft a reply saying I'll be there");
        assertThat(prompt.userPrompt()).doesNotContain("Respond in");
        assertThat(prompt.systemPrompt()).containsIgnoringCase("voice");
    }

    @Test
    void forVoiceCommand_withLanguage_includesRespondInInstruction() {
        PreparedPrompt prompt = promptFactory.forVoiceCommand("What's a polite way to decline?", "Spanish");

        assertThat(prompt.userPrompt()).contains("Respond in Spanish.");
    }

    @Test
    void withReferenceContext_appendsItAsALabeledBlock_withoutTouchingTheSystemPrompt() {
        PreparedPrompt base = promptFactory.forGenerateReply("Can we confirm the meeting?", null, null);

        PreparedPrompt augmented = promptFactory.withReferenceContext(
                base, "Our pricing: Basic $10/mo, Pro $30/mo.");

        assertThat(augmented.systemPrompt()).isEqualTo(base.systemPrompt());
        assertThat(augmented.userPrompt()).contains("Can we confirm the meeting?");
        assertThat(augmented.userPrompt()).contains("Reference material");
        assertThat(augmented.userPrompt()).contains("Our pricing: Basic $10/mo, Pro $30/mo.");
    }

    @Test
    void withReferenceContext_returnsThePromptUnchanged_whenReferenceContextIsNull() {
        PreparedPrompt base = promptFactory.forGenerateReply("Can we confirm the meeting?", null, null);

        PreparedPrompt result = promptFactory.withReferenceContext(base, null);

        assertThat(result).isEqualTo(base);
    }

    @Test
    void withReferenceContext_returnsThePromptUnchanged_whenReferenceContextIsBlank() {
        PreparedPrompt base = promptFactory.forGenerateReply("Can we confirm the meeting?", null, null);

        PreparedPrompt result = promptFactory.withReferenceContext(base, "   ");

        assertThat(result).isEqualTo(base);
    }
}
