package com.intellimail.mail.client;

import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.prompt.PreparedPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NOTE ON RISK: this test mocks Spring AI's ChatClient/ChatResponse/Generation/
 * Usage types directly (rather than constructing real instances) specifically
 * to avoid depending on their exact constructor signatures, which differ
 * across Spring AI 1.0.x patch releases and could not be verified against a
 * live build in this environment (no Maven available here). If this test
 * fails to compile, the most likely culprit is {@code Usage.getPromptTokens()}
 * / {@code getTotalTokens()} returning {@code Long} rather than {@code Integer}
 * in your resolved Spring AI version — change the stubbed return values here
 * (and the cast in {@code AzureOpenAiClient.toResult}) accordingly.
 */
@ExtendWith(MockitoExtension.class)
class AzureOpenAiClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private AzureOpenAiClient azureOpenAiClient;

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties(0.7, 1024, true, new AiProperties.Retry(3, 0));
        azureOpenAiClient = new AzureOpenAiClient(chatClient, aiProperties);
    }

    @Test
    void generate_mapsChatResponseIntoAiGenerationResult() {
        ChatResponse chatResponse = mockSuccessfulChatResponse("Sure, Tuesday works for me.", "gpt-4o", 120, 200);
        when(chatClient.prompt().system(anyString()).user(anyString()).options(any()).call().chatResponse())
                .thenReturn(chatResponse);

        AiGenerationResult result = azureOpenAiClient.generate(new PreparedPrompt("system", "user"));

        assertThat(result.content()).isEqualTo("Sure, Tuesday works for me.");
        assertThat(result.model()).isEqualTo("gpt-4o");
        assertThat(result.promptTokens()).isEqualTo(120);
        assertThat(result.totalTokens()).isEqualTo(200);
        assertThat(result.completionTokens()).isEqualTo(80);
    }

    @Test
    void generate_retriesTransientFailure_thenSucceeds() {
        ChatResponse chatResponse = mockSuccessfulChatResponse("Recovered reply", "gpt-4o", 10, 20);
        when(chatClient.prompt().system(anyString()).user(anyString()).options(any()).call().chatResponse())
                .thenThrow(new RuntimeException("transient azure error"))
                .thenReturn(chatResponse);

        AiGenerationResult result = azureOpenAiClient.generate(new PreparedPrompt("system", "user"));

        assertThat(result.content()).isEqualTo("Recovered reply");
    }

    @Test
    void generate_wrapsExhaustedRetries_inAiGenerationException() {
        when(chatClient.prompt().system(anyString()).user(anyString()).options(any()).call().chatResponse())
                .thenThrow(new RuntimeException("always fails"));

        assertThatThrownBy(() -> azureOpenAiClient.generate(new PreparedPrompt("system", "user")))
                .isInstanceOf(AiGenerationException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private ChatResponse mockSuccessfulChatResponse(String content, String model, int promptTokens, int totalTokens) {
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn(content);

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(promptTokens);
        when(usage.getTotalTokens()).thenReturn(totalTokens);

        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getModel()).thenReturn(model);
        when(metadata.getUsage()).thenReturn(usage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatResponse.getMetadata()).thenReturn(metadata);
        return chatResponse;
    }
}
