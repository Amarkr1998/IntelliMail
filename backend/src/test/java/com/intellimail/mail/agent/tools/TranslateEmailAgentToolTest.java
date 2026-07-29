package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailTranslateRequest;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateEmailAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private TranslateEmailAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new TranslateEmailAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), null);
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void translateEmail_delegatesToEmailService_withRequestedLanguage() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.TRANSLATE,
                "Hallo, wie geht es dir?", "gpt-4o", 1, 20, 200L, false, Instant.now());
        when(emailService.translate(eq(userId), any(EmailTranslateRequest.class))).thenReturn(response);

        String result = tool.translateEmail("Hello, how are you?", "German");

        assertThat(result).isEqualTo("Hallo, wie geht es dir?");
        ArgumentCaptor<EmailTranslateRequest> captor = ArgumentCaptor.forClass(EmailTranslateRequest.class);
        verify(emailService).translate(eq(userId), captor.capture());
        assertThat(captor.getValue().targetLanguage()).isEqualTo("German");
        verify(stepRecorder).record(eq("translateEmail"), any(), eq("Hallo, wie geht es dir?"), eq(AgentStepStatus.SUCCESS));
    }
}
