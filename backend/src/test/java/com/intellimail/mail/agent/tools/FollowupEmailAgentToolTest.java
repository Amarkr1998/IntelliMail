package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailFollowupRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
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
class FollowupEmailAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private FollowupEmailAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new FollowupEmailAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void followupEmail_delegatesToEmailService_andReturnsFollowupVerbatim() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.FOLLOWUP,
                "Just checking in on this.", "gpt-4o", 1, 12, 120L, false, Instant.now());
        when(emailService.followup(eq(userId), any(EmailFollowupRequest.class))).thenReturn(response);

        String result = tool.followupEmail("Original unanswered email", "Nudge politely");

        assertThat(result).isEqualTo("Just checking in on this.");
        verify(stepRecorder).record(eq("followupEmail"), any(), eq("Just checking in on this."), eq(AgentStepStatus.SUCCESS));
    }
}
