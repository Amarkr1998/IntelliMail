package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentPendingActionHolder;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.PendingActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proposes saving text as a reusable prompt template - the only mutating
 * action in this tool set. Deliberately does NOT call PromptTemplateService:
 * it only records a proposal (via {@link AgentPendingActionHolder}) that the
 * user must explicitly confirm through POST /api/agent/tasks/{id}/confirm
 * before anything is actually persisted.
 */
@Component
@RequiredArgsConstructor
public class SaveTemplateAgentTool {

    private static final String TOOL_NAME = "proposeSaveTemplate";

    private final AgentPendingActionHolder pendingActionHolder;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Proposes saving a piece of text as a reusable prompt template. This does NOT
            save anything yet - it only records a proposal that the user must explicitly
            confirm in the UI before a template is actually created. Use only when the
            user has explicitly asked to save/remember something as a template.""")
    public String proposeSaveTemplate(
            @ToolParam(description = "Short template name") String name,
            @ToolParam(description = "One-sentence description of the template", required = false) String description,
            @ToolParam(description = "Category - one of the RequestType enum values, e.g. GENERATE_REPLY, "
                    + "PROFESSIONAL_REWRITE, CUSTOM_PROMPT") String category,
            @ToolParam(description = "The prompt text to save as the template") String promptText,
            @ToolParam(description = "Optional system prompt override for this template", required = false)
            String systemPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description == null ? "" : description);
        payload.put("category", category);
        payload.put("promptText", promptText);
        payload.put("systemPrompt", systemPrompt == null ? "" : systemPrompt);
        payload.put("isPublic", false);

        pendingActionHolder.propose(PendingActionType.SAVE_TEMPLATE, payload);
        String result = "Proposed saving this as a template titled '" + name + "' - awaiting user confirmation.";
        stepRecorder.record(TOOL_NAME, name, result, AgentStepStatus.SUCCESS);
        return result;
    }
}
