package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSubjectRequest;
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
class SubjectLineAgentToolTest {

    @Mock
    private EmailService emailService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private SubjectLineAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new SubjectLineAgentTool(emailService, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), null);
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void subjectLine_delegatesToEmailService_andReturnsSubjectVerbatim() {
        EmailReplyResponse response = new EmailReplyResponse(
                UUID.randomUUID(), UUID.randomUUID(), RequestType.SUBJECT_LINE,
                "Re: Tuesday meeting", "gpt-4o", 1, 10, 100L, false, Instant.now());
        when(emailService.subjectLine(eq(userId), any(EmailSubjectRequest.class))).thenReturn(response);

        String result = tool.subjectLine("Body text about a meeting");

        assertThat(result).isEqualTo("Re: Tuesday meeting");
        verify(stepRecorder).record(eq("subjectLine"), any(), eq("Re: Tuesday meeting"), eq(AgentStepStatus.SUCCESS));
    }
}
