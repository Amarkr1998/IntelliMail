package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailCustomRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Wraps {@code EmailService.custom} as an agent tool - composes a new email
 * from scratch (not a reply to existing text), for the categories
 * {@code EmailCustomRequest} supports. Mirrors the same allowed-category set
 * {@code @ValidCustomRequestType} enforces at the controller layer, since
 * calling {@code EmailService} directly here bypasses that bean validation.
 */
@Component
@RequiredArgsConstructor
public class ComposeFromScratchAgentTool {

    private static final String TOOL_NAME = "composeFromScratch";

    private static final Set<RequestType> ALLOWED = EnumSet.of(
            RequestType.MEETING_REQUEST,
            RequestType.THANK_YOU,
            RequestType.APOLOGY,
            RequestType.SALES,
            RequestType.HR,
            RequestType.MARKETING,
            RequestType.COLD_OUTREACH,
            RequestType.CUSTOM_PROMPT
    );

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Composes a brand-new email from scratch (not a reply to something the user
            received). Use this for goals like drafting a meeting request, a thank-you note,
            an apology, a sales pitch, an HR email, marketing copy, cold outreach, or any
            fully custom-prompted email. Returns the drafted text verbatim - relay it to the
            user as-is.""")
    public String composeFromScratch(
            @ToolParam(description = "One of: MEETING_REQUEST, THANK_YOU, APOLOGY, SALES, HR, MARKETING, "
                    + "COLD_OUTREACH, CUSTOM_PROMPT") String category,
            @ToolParam(description = "Background context for the email, e.g. who it's to and what it's about")
            String context,
            @ToolParam(description = "Specific instructions or the custom prompt to follow", required = false)
            String customPrompt,
            @ToolParam(description = "Id of a saved template to use, from the listTemplates tool", required = false)
            String promptTemplateId) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            RequestType requestType = RequestType.valueOf(category.trim().toUpperCase());
            if (!ALLOWED.contains(requestType)) {
                throw new IllegalArgumentException("category must be one of " + ALLOWED);
            }
            EmailReplyResponse response = emailService.custom(ctx.userId(), new EmailCustomRequest(
                    requestType, context, customPrompt, GenerateReplyAgentTool.parseUuid(promptTemplateId), ctx.referenceContext()));
            stepRecorder.record(TOOL_NAME, context, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, context, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "composeFromScratch failed: " + ex.getMessage();
        }
    }
}
