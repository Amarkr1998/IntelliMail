package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "intellimail.ai")
public record AiProperties(
        double defaultTemperature,
        int defaultMaxTokens,
        // Some Azure OpenAI deployments (newer reasoning-tier models) reject any
        // non-default temperature and reject max_tokens outright, requiring
        // max_completion_tokens instead (which Spring AI 1.0.0 doesn't yet expose).
        // Set to false for such deployments so AzureOpenAiClient omits both
        // parameters and lets the model use its own defaults.
        @DefaultValue("true") boolean sendSamplingParameters,
        Retry retry
) {
    public record Retry(int maxAttempts, long backoffMs) {
    }
}
