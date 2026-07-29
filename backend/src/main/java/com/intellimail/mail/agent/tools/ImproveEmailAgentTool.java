package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailImproveRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Wraps {@code EmailService.improve} as an agent tool - covers rewriting
 * (professional/friendly/formal/casual), grammar correction, expanding, and
 * shortening, since they all share the same {@code style}-driven endpoint.
 */
@Component
@RequiredArgsConstructor
public class ImproveEmailAgentTool {

    private static final String TOOL_NAME = "improveEmail";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Rewrites, corrects, expands, or shortens a piece of email text. Use this for
            goals like "make this more professional", "fix the grammar", "make it
            friendlier", "shorten this", or "expand this". Returns the improved text
            verbatim - relay it to the user as-is.""")
    public String improveEmail(
            @ToolParam(description = "The email text to improve") String content,
            @ToolParam(description = "One of: PROFESSIONAL_REWRITE, FRIENDLY_REWRITE, FORMAL_REWRITE, "
                    + "CASUAL_REWRITE, GRAMMAR_CORRECTION, EXPAND, SHORTEN")
            String style) {
        try {
            RequestType styleType = RequestType.valueOf(style.trim().toUpperCase());
            AgentExecutionContext.Context ctx = AgentExecutionContext.current();
            EmailReplyResponse response = emailService.improve(ctx.userId(),
                    new EmailImproveRequest(content, styleType, ctx.referenceContext()));
            stepRecorder.record(TOOL_NAME, content, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, content, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "improveEmail failed: " + ex.getMessage();
        }
    }
}
