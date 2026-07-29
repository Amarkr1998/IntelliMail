package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSummarizeRequest;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Wraps {@code EmailService.summarize} as an agent tool. */
@Component
@RequiredArgsConstructor
public class SummarizeEmailAgentTool {

    private static final String TOOL_NAME = "summarizeEmail";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Summarizes a long piece of email text into a short summary. Returns the
            summary verbatim - relay it to the user as-is.""")
    public String summarizeEmail(@ToolParam(description = "The email text to summarize") String content) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            EmailReplyResponse response = emailService.summarize(ctx.userId(), new EmailSummarizeRequest(content, ctx.referenceContext()));
            stepRecorder.record(TOOL_NAME, content, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, content, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "summarizeEmail failed: " + ex.getMessage();
        }
    }
}
