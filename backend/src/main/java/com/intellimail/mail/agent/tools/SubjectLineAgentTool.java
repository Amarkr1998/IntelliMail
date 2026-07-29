package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSubjectRequest;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Wraps {@code EmailService.subjectLine} as an agent tool. */
@Component
@RequiredArgsConstructor
public class SubjectLineAgentTool {

    private static final String TOOL_NAME = "subjectLine";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Generates a subject line for a given email body. Returns the subject line
            verbatim - relay it to the user as-is.""")
    public String subjectLine(@ToolParam(description = "The email body to generate a subject line for") String content) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            EmailReplyResponse response = emailService.subjectLine(ctx.userId(), new EmailSubjectRequest(content, ctx.referenceContext()));
            stepRecorder.record(TOOL_NAME, content, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, content, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "subjectLine failed: " + ex.getMessage();
        }
    }
}
