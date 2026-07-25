package com.intellimail.mail.prompt;

import com.intellimail.mail.enums.RequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Assembles the final (system, user) prompt pair for each AI endpoint,
 * rendering the user-turn text via Spring AI's {@link PromptTemplate}.
 * A caller-supplied {@code overrideSystemPrompt} (sourced from a saved
 * {@code PromptTemplate} entity in Module 7) always wins over the built-in
 * {@link SystemPromptCatalog} default when present.
 */
@Component
@RequiredArgsConstructor
public class PromptFactory {

    private static final String CONTEXTUAL_ACTION_TEMPLATE = """
            Original email:
            ---
            {content}
            ---
            {instructionsBlock}
            Write the reply now.""";

    private static final String CONTENT_ONLY_TEMPLATE = """
            Email:
            ---
            {content}
            ---""";

    private static final String TRANSLATE_TEMPLATE = """
            Target language: {targetLanguage}

            Email:
            ---
            {content}
            ---""";

    private static final String GENERATOR_TEMPLATE = """
            Context:
            ---
            {context}
            ---
            {customPromptBlock}""";

    private final SystemPromptCatalog systemPromptCatalog;

    public PreparedPrompt forGenerateReply(String originalContent, String instructions, String overrideSystemPrompt) {
        String system = resolveSystemPrompt(RequestType.GENERATE_REPLY, overrideSystemPrompt);
        String user = new PromptTemplate(CONTEXTUAL_ACTION_TEMPLATE).render(Map.of(
                "content", originalContent,
                "instructionsBlock", instructionsBlock(instructions)));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forRewrite(RequestType style, String content, String overrideSystemPrompt) {
        String system = resolveSystemPrompt(style, overrideSystemPrompt);
        String user = new PromptTemplate(CONTENT_ONLY_TEMPLATE).render(Map.of("content", content));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forSummarize(String content) {
        String system = systemPromptCatalog.systemPromptFor(RequestType.SUMMARIZE);
        String user = new PromptTemplate(CONTENT_ONLY_TEMPLATE).render(Map.of("content", content));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forSubjectLine(String content) {
        String system = systemPromptCatalog.systemPromptFor(RequestType.SUBJECT_LINE);
        String user = new PromptTemplate(CONTENT_ONLY_TEMPLATE).render(Map.of("content", content));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forTranslate(String content, String targetLanguage) {
        String system = systemPromptCatalog.systemPromptFor(RequestType.TRANSLATE);
        String user = new PromptTemplate(TRANSLATE_TEMPLATE).render(Map.of(
                "content", content,
                "targetLanguage", targetLanguage));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forFollowup(String originalContent, String instructions) {
        String system = systemPromptCatalog.systemPromptFor(RequestType.FOLLOWUP);
        String user = new PromptTemplate(CONTEXTUAL_ACTION_TEMPLATE).render(Map.of(
                "content", originalContent,
                "instructionsBlock", instructionsBlock(instructions)));
        return new PreparedPrompt(system, user);
    }

    public PreparedPrompt forCustomGenerator(RequestType requestType, String context, String customPrompt, String overrideSystemPrompt) {
        String system = resolveSystemPrompt(requestType, overrideSystemPrompt);
        String user = new PromptTemplate(GENERATOR_TEMPLATE).render(Map.of(
                "context", context,
                "customPromptBlock", customPromptBlock(customPrompt)));
        return new PreparedPrompt(system, user);
    }

    private String resolveSystemPrompt(RequestType requestType, String overrideSystemPrompt) {
        return (overrideSystemPrompt == null || overrideSystemPrompt.isBlank())
                ? systemPromptCatalog.systemPromptFor(requestType)
                : overrideSystemPrompt;
    }

    private String instructionsBlock(String instructions) {
        return (instructions == null || instructions.isBlank())
                ? ""
                : "Additional instructions from the user: " + instructions;
    }

    private String customPromptBlock(String customPrompt) {
        return (customPrompt == null || customPrompt.isBlank())
                ? ""
                : "Additional instructions from the user: " + customPrompt;
    }
}
