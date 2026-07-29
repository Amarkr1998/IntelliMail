package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSummarizeRequest;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SummarizeEmailAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private SummarizeEmailAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new SummarizeEmailAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void summarizeEmail_delegatesToEmailService_andReturnsSummaryVerbatim() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.SUMMARIZE,
                "Short summary.", "gpt-4o", 1, 15, 150L, false, Instant.now());
        when(emailService.summarize(eq(userId), any(EmailSummarizeRequest.class))).thenReturn(response);

        String result = tool.summarizeEmail("A very long email body...");

        assertThat(result).isEqualTo("Short summary.");
        verify(stepRecorder).record(eq("summarizeEmail"), any(), eq("Short summary."), eq(AgentStepStatus.SUCCESS));
    }
}
