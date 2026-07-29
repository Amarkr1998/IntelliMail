package com.intellimail.mail.config;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
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

    /**
     * The same deployment-safe options {@link com.intellimail.mail.client.AzureOpenAiClient}
     * builds internally - any other code calling a ChatClient built against this
     * app's configured Azure OpenAI deployment (the agent orchestrator/reflection
     * pass) needs the same override, or deployments that reject non-default
     * temperature (confirmed live: "Only the default (1) value is supported")
     * fail every call.
     *
     * <p>Built as {@link ToolCallingChatOptions} rather than a plain {@link ChatOptions}
     * - confirmed live that passing a plain ChatOptions via {@code .options(...)} on a
     * ChatClient call silently drops the tools registered via {@code .defaultTools(...)},
     * since tool-callback merging only happens when the runtime options object itself
     * carries a (possibly empty) tool-callback list to merge into.
     */
    public ChatOptions toChatOptions() {
        if (!sendSamplingParameters) {
            return ToolCallingChatOptions.builder().temperature(1.0).build();
        }
        return ToolCallingChatOptions.builder().temperature(defaultTemperature).maxTokens(defaultMaxTokens).build();
    }
}
