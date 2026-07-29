package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailImproveRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImproveEmailAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private ImproveEmailAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new ImproveEmailAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void improveEmail_parsesStyle_andDelegatesToEmailService() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.GRAMMAR_CORRECTION,
                "Fixed text.", "gpt-4o", 1, 30, 300L, false, Instant.now());
        when(emailService.improve(eq(userId), any(EmailImproveRequest.class))).thenReturn(response);

        String result = tool.improveEmail("Some typo-ridden text", "grammar_correction");

        assertThat(result).isEqualTo("Fixed text.");
        ArgumentCaptor<EmailImproveRequest> captor = ArgumentCaptor.forClass(EmailImproveRequest.class);
        verify(emailService).improve(eq(userId), captor.capture());
        assertThat(captor.getValue().style()).isEqualTo(RequestType.GRAMMAR_CORRECTION);
        verify(stepRecorder).record(eq("improveEmail"), any(), eq("Fixed text."), eq(AgentStepStatus.SUCCESS));
    }

    @Test
    void improveEmail_withInvalidStyle_recordsFailedStep_andNeverCallsEmailService() {
        String result = tool.improveEmail("content", "NOT_A_REAL_STYLE");

        assertThat(result).contains("improveEmail failed");
        verify(stepRecorder).record(eq("improveEmail"), any(), any(), eq(AgentStepStatus.FAILED));
        verifyNoInteractions(emailService);
    }
}
