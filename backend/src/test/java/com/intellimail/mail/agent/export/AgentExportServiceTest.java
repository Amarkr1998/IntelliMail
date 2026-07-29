package com.intellimail.mail.agent.export;

import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.AgentTaskStep;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.AgentTaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExportServiceTest {

    private final AgentExportService exportService = new AgentExportService();

    @Test
    void renderTaskAsPdf_producesRealPdfBytes_forMarkdownWithHeadingsListsAndTable() {
        AgentTask task = AgentTask.builder()
                .user(User.builder().fullName("User").email("user@intellimail.com").password("hashed").build())
                .goal("Draft a reply and summarize the pricing")
                .status(AgentTaskStatus.COMPLETED)
                .conversationId(UUID.randomUUID())
                .finalResult("""
                        ## Reply

                        Hi Alex, **Tuesday at 3pm** works for me.

                        | Tier | Price |
                        |------|-------|
                        | Basic | $19 |
                        | Pro | $49 |

                        - Confirmed for Tuesday
                        - Will send calendar invite
                        """)
                .build();
        task.setId(UUID.randomUUID());

        AgentTaskStep step = AgentTaskStep.builder()
                .agentTask(task)
                .stepNumber(1)
                .toolName("generateReply")
                .inputSummary("Can we meet Tuesday?")
                .outputSummary("Hi Alex, Tuesday at 3pm works for me.")
                .status(AgentStepStatus.SUCCESS)
                .build();

        byte[] pdf = exportService.renderTaskAsPdf(task, List.of(step));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000); // a trivial/broken render would be near-empty
    }

    @Test
    void renderTaskAsPdf_withNoSteps_stillProducesAValidPdf() {
        AgentTask task = AgentTask.builder()
                .user(User.builder().fullName("User").email("user@intellimail.com").password("hashed").build())
                .goal("Summarize this")
                .status(AgentTaskStatus.COMPLETED)
                .conversationId(UUID.randomUUID())
                .finalResult("A short plain-text result with no markdown at all.")
                .build();
        task.setId(UUID.randomUUID());

        byte[] pdf = exportService.renderTaskAsPdf(task, List.of());

        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
