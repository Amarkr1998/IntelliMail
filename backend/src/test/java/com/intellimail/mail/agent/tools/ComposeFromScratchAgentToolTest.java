package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailCustomRequest;
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
class ComposeFromScratchAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private ComposeFromScratchAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new ComposeFromScratchAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), "Background: price list attached");
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void composeFromScratch_delegatesToEmailServiceCustom_withReferenceContextAndTemplateId() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.SALES,
                "Dear prospect, ...", "gpt-4o", 1, 40, 400L, false, Instant.now());
        when(emailService.custom(eq(userId), any(EmailCustomRequest.class))).thenReturn(response);
        UUID templateId = UUID.randomUUID();

        String result = tool.composeFromScratch("sales", "New prospect, interested in our platform", "Keep it short",
                templateId.toString());

        assertThat(result).isEqualTo("Dear prospect, ...");
        ArgumentCaptor<EmailCustomRequest> captor = ArgumentCaptor.forClass(EmailCustomRequest.class);
        verify(emailService).custom(eq(userId), captor.capture());
        assertThat(captor.getValue().requestType()).isEqualTo(RequestType.SALES);
        assertThat(captor.getValue().promptTemplateId()).isEqualTo(templateId);
        assertThat(captor.getValue().referenceContext()).isEqualTo("Background: price list attached");
        verify(stepRecorder).record(eq("composeFromScratch"), any(), eq("Dear prospect, ..."), eq(AgentStepStatus.SUCCESS));
    }

    @Test
    void composeFromScratch_withDisallowedCategory_recordsFailedStep_andNeverCallsEmailService() {
        String result = tool.composeFromScratch("GENERATE_REPLY", "context", null, null);

        assertThat(result).contains("composeFromScratch failed");
        verify(stepRecorder).record(eq("composeFromScratch"), any(), any(), eq(AgentStepStatus.FAILED));
        verifyNoInteractions(emailService);
    }
}
