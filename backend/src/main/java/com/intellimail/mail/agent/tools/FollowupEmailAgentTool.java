package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailFollowupRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Wraps {@code EmailService.followup} as an agent tool. */
@Component
@RequiredArgsConstructor
public class FollowupEmailAgentTool {

    private static final String TOOL_NAME = "followupEmail";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Drafts a follow-up email for a thread that received no response. Use this
            when the goal is to nudge/follow up on an email that was already sent.
            Returns the drafted follow-up verbatim - relay it to the user as-is.""")
    public String followupEmail(
            @ToolParam(description = "Full text of the original email that received no response") String originalContent,
            @ToolParam(description = "Tone or points to make in the follow-up", required = false) String instructions) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            EmailReplyResponse response = emailService.followup(ctx.userId(),
                    new EmailFollowupRequest(originalContent, instructions, null));
            stepRecorder.record(TOOL_NAME, originalContent, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, originalContent, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "followupEmail failed: " + ex.getMessage();
        }
    }
}
