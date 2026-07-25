package com.intellimail.mail.client;

import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.util.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Thin wrapper around Spring AI's {@link ChatClient} for the Azure OpenAI
 * deployment configured in application.yml. Owns three concerns the raw
 * ChatClient doesn't: applying per-request temperature/max-tokens, retrying
 * transient failures with linear backoff (via {@link RetryExecutor}), and
 * translating exhausted retries into the domain-level
 * {@link AiGenerationException} that {@code GlobalExceptionHandler} (Module 5)
 * already knows how to turn into a 502 response.
 *
 * <p><b>Known Spring AI 1.0.0 limitation:</b> newer Azure OpenAI reasoning-tier
 * deployments reject the legacy {@code max_tokens} request parameter
 * ({@code "Unsupported parameter: 'max_tokens' ... Use 'max_completion_tokens'
 * instead"}) and reject any non-default {@code temperature}
 * ({@code "Only the default (1) value is supported"}) - both discovered by
 * running a real request against a live Azure OpenAI resource.
 * {@code AzureOpenAiChatOptions} in Spring AI 1.0.0 GA has no equivalent for
 * {@code max_completion_tokens}, so there's no way to express "use this max
 * length" against such a deployment at all yet. Worse, Spring AI's own
 * {@code AzureOpenAiChatProperties} unconditionally hardcodes a default
 * temperature of {@code 0.7} into every auto-configured request regardless of
 * YAML config - so "don't send a temperature" isn't achievable by omission;
 * the only way to satisfy "only the default (1) is supported" is to
 * explicitly send {@code 1.0}, overriding Spring AI's baked-in 0.7.
 * {@link AiProperties#sendSamplingParameters()} makes this a per-environment
 * toggle: {@code true} (default) sends temperature/max-tokens as configured,
 * for deployments that support custom values; {@code false} sends
 * {@code temperature=1.0} and omits max-tokens, for deployments that don't.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AzureOpenAiClient {

    private final ChatClient chatClient;
    private final AiProperties aiProperties;

    public AiGenerationResult generate(PreparedPrompt prompt) {
        return generate(prompt, aiProperties.defaultTemperature(), aiProperties.defaultMaxTokens());
    }

    public AiGenerationResult generate(PreparedPrompt prompt, double temperature, int maxTokens) {
        int maxAttempts = aiProperties.retry().maxAttempts();
        long backoffMs = aiProperties.retry().backoffMs();

        try {
            return RetryExecutor.execute(
                    () -> callOnce(prompt, temperature, maxTokens),
                    maxAttempts,
                    backoffMs,
                    "Azure OpenAI chat completion");
        } catch (RuntimeException ex) {
            throw new AiGenerationException("Azure OpenAI request failed after " + maxAttempts + " attempt(s)", ex);
        }
    }

    /** Streams tokens as they arrive — used for the live-typing effect in the Compose Assistant UI. */
    public Flux<String> generateStream(PreparedPrompt prompt, double temperature, int maxTokens) {
        ChatOptions options = buildOptions(temperature, maxTokens);

        return chatClient.prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt())
                .options(options)
                .stream()
                .content()
                .doOnError(ex -> log.error("Azure OpenAI streaming call failed", ex));
    }

    private AiGenerationResult callOnce(PreparedPrompt prompt, double temperature, int maxTokens) {
        long start = System.currentTimeMillis();

        ChatOptions options = buildOptions(temperature, maxTokens);

        ChatResponse response = chatClient.prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt())
                .options(options)
                .call()
                .chatResponse();

        long latencyMs = System.currentTimeMillis() - start;
        return toResult(response, latencyMs);
    }

    private ChatOptions buildOptions(double temperature, int maxTokens) {
        if (!aiProperties.sendSamplingParameters()) {
            // 1.0 is not "no preference" here - it explicitly overrides Spring AI's
            // hardcoded 0.7 default, which some deployments reject outright.
            return ChatOptions.builder().temperature(1.0).build();
        }
        return ChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    private AiGenerationResult toResult(ChatResponse response, long latencyMs) {
        String content = response.getResult().getOutput().getText();
        String model = response.getMetadata().getModel();
        Usage usage = response.getMetadata().getUsage();

        // Usage token-count getters are read as Number rather than a specific boxed
        // type since their exact return type (Integer vs Long) varies across Spring AI
        // point releases; .intValue() is safe regardless of which one is in use.
        Integer promptTokens = usage != null && usage.getPromptTokens() != null
                ? ((Number) usage.getPromptTokens()).intValue() : null;
        Integer totalTokens = usage != null && usage.getTotalTokens() != null
                ? ((Number) usage.getTotalTokens()).intValue() : null;
        Integer completionTokens = (promptTokens != null && totalTokens != null)
                ? totalTokens - promptTokens : null;

        return new AiGenerationResult(content, model, promptTokens, completionTokens, totalTokens, latencyMs);
    }
}
