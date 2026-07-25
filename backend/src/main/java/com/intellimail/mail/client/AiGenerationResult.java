package com.intellimail.mail.client;

/** Everything the service layer needs from a completed (non-streamed) AI call. */
public record AiGenerationResult(
        String content,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long latencyMs
) {
}
