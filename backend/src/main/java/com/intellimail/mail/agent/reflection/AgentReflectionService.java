package com.intellimail.mail.agent.reflection;

import com.intellimail.mail.agent.prompt.AgentSystemPrompts;
import com.intellimail.mail.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * One bounded self-check pass over an agent's result, not an open-ended
 * verification framework. Uses the plain (tool-less) ChatClient bean since
 * judging a result needs no tool access.
 */
@Service
@RequiredArgsConstructor
public class AgentReflectionService {

    private final @Qualifier("chatClient") ChatClient chatClient;
    private final AiProperties aiProperties;

    public record ReflectionResult(boolean pass, String reason) {
    }

    public ReflectionResult reflect(String goal, String candidateResult) {
        if (candidateResult == null || candidateResult.isBlank()) {
            return new ReflectionResult(false, "Result was empty");
        }

        String verdict = chatClient.prompt()
                .system(AgentSystemPrompts.REFLECTION_SYSTEM_PROMPT)
                .user("Goal: " + goal + "\n\nResult:\n" + candidateResult)
                .options(aiProperties.toChatOptions())
                .call()
                .content();

        if (verdict == null || verdict.isBlank()) {
            return new ReflectionResult(true, null);
        }
        String trimmed = verdict.trim();
        if (trimmed.startsWith("PASS")) {
            return new ReflectionResult(true, null);
        }
        return new ReflectionResult(false, trimmed.replaceFirst("^FAIL:?\\s*", ""));
    }
}
