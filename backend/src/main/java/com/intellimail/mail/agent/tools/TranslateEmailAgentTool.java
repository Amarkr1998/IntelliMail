package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailTranslateRequest;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Wraps {@code EmailService.translate} as an agent tool. */
@Component
@RequiredArgsConstructor
public class TranslateEmailAgentTool {

    private static final String TOOL_NAME = "translateEmail";

    private final EmailService emailService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Translates a piece of email text into another language. Returns the
            translated text verbatim - relay it to the user as-is.""")
    public String translateEmail(
            @ToolParam(description = "The email text to translate") String content,
            @ToolParam(description = "Target language, e.g. 'German' or 'Spanish'") String targetLanguage) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            EmailReplyResponse response = emailService.translate(ctx.userId(),
                    new EmailTranslateRequest(content, targetLanguage, null));
            stepRecorder.record(TOOL_NAME, content, response.content(), AgentStepStatus.SUCCESS);
            return response.content();
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, content, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "translateEmail failed: " + ex.getMessage();
        }
    }
}
