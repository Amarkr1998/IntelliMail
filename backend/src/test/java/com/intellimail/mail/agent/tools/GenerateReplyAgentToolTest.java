package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateReplyAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private GenerateReplyAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new GenerateReplyAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), null);
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void generateReply_delegatesToEmailService_andRecordsSuccessStep() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.GENERATE_REPLY,
                "Sure, Tuesday works.", "gpt-4o", 1, 50, 500L, false, Instant.now());
        when(emailService.generateReply(eq(userId), any(EmailGenerateRequest.class))).thenReturn(response);

        String result = tool.generateReply("Can we meet Tuesday?", "Be polite", null);

        assertThat(result).isEqualTo("Sure, Tuesday works.");
        ArgumentCaptor<EmailGenerateRequest> captor = ArgumentCaptor.forClass(EmailGenerateRequest.class);
        verify(emailService).generateReply(eq(userId), captor.capture());
        assertThat(captor.getValue().originalContent()).isEqualTo("Can we meet Tuesday?");
        assertThat(captor.getValue().instructions()).isEqualTo("Be polite");
        verify(stepRecorder).record(eq("generateReply"), any(), eq("Sure, Tuesday works."), eq(AgentStepStatus.SUCCESS));
    }

    @Test
    void generateReply_threadsReferenceContext_andParsesPromptTemplateId() {
        AgentExecutionContext.clear();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), "Price list: Basic $10, Pro $30");
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.GENERATE_REPLY,
                "Sure.", "gpt-4o", 1, 10, 100L, false, Instant.now());
        when(emailService.generateReply(eq(userId), any(EmailGenerateRequest.class))).thenReturn(response);
        UUID templateId = UUID.randomUUID();

        tool.generateReply("content", null, templateId.toString());

        ArgumentCaptor<EmailGenerateRequest> captor = ArgumentCaptor.forClass(EmailGenerateRequest.class);
        verify(emailService).generateReply(eq(userId), captor.capture());
        assertThat(captor.getValue().referenceContext()).isEqualTo("Price list: Basic $10, Pro $30");
        assertThat(captor.getValue().promptTemplateId()).isEqualTo(templateId);
    }

    @Test
    void generateReply_whenEmailServiceThrows_recordsFailedStep_andReturnsErrorString_ratherThanPropagating() {
        when(emailService.generateReply(eq(userId), any(EmailGenerateRequest.class)))
                .thenThrow(new AiGenerationException("Azure OpenAI request failed"));

        String result = tool.generateReply("content", null, null);

        assertThat(result).contains("generateReply failed");
        verify(stepRecorder).record(eq("generateReply"), any(), any(), eq(AgentStepStatus.FAILED));
    }
}
