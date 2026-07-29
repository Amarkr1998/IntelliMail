package com.intellimail.mail.agent.reflection;

import com.intellimail.mail.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentReflectionServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private final AiProperties aiProperties = new AiProperties(0.7, 1024, true, new AiProperties.Retry(3, 500));

    private AgentReflectionService reflectionService;

    @BeforeEach
    void setUp() {
        reflectionService = new AgentReflectionService(chatClient, aiProperties);
    }

    @Test
    void reflect_withEmptyResult_failsWithoutCallingTheModel() {
        AgentReflectionService.ReflectionResult result = reflectionService.reflect("Draft a reply", "  ");

        assertThat(result.pass()).isFalse();
        assertThat(result.reason()).isEqualTo("Result was empty");
    }

    @Test
    void reflect_whenModelRespondsPass_returnsPassingResult() {
        when(chatClient.prompt().system(anyString()).user(anyString()).options(any()).call().content()).thenReturn("PASS");

        AgentReflectionService.ReflectionResult result = reflectionService.reflect("Draft a reply", "Here is the reply.");

        assertThat(result.pass()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    void reflect_whenModelRespondsFail_returnsReasonWithoutThePrefix() {
        when(chatClient.prompt().system(anyString()).user(anyString()).options(any()).call().content())
                .thenReturn("FAIL: contains a leftover placeholder");

        AgentReflectionService.ReflectionResult result = reflectionService.reflect("Draft a reply", "Dear [Name], ...");

        assertThat(result.pass()).isFalse();
        assertThat(result.reason()).isEqualTo("contains a leftover placeholder");
    }
}
