package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Wraps {@code EmailService.generateReply} as an agent tool. */
@Component
@RequiredArgsConstructor
public class GenerateReplyAgentTool {

    private static final String TOOL_NAME = "generateReply";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Drafts a reply to an existing email thread the user received. Use this when
            the goal is to respond to an email someone sent. Returns the drafted reply
            text verbatim - relay it to the user as-is.""")
    public String generateReply(
            @ToolParam(description = "Full text of the email being replied to") String originalContent,
            @ToolParam(description = "Tone or points to make in the reply, e.g. 'politely decline'", required = false)
            String instructions) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            EmailReplyResponse response = emailService.generateReply(ctx.userId(),
                    new EmailGenerateRequest(originalContent, instructions, null, null));
            stepRecorder.record(TOOL_NAME, originalContent, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, originalContent, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "generateReply failed: " + ex.getMessage();
        }
    }
}
