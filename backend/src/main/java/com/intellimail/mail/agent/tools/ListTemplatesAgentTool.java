package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Read-only lookup over the user's own saved templates, so a goal like "reply
 * using my Decline Meeting template" can find the template's id and pass it
 * to {@code generateReply}/{@code composeFromScratch}. No new repository
 * query needed - {@code PromptTemplateService.getTemplates} already exists;
 * this just fetches a page and filters by keyword in memory, proportionate
 * for a per-user template list that's realistically small.
 */
@Component
@RequiredArgsConstructor
public class ListTemplatesAgentTool {

    private static final String TOOL_NAME = "listTemplates";
    private static final int FETCH_SIZE = 50;
    private static final int MAX_RESULTS = 10;

    private final PromptTemplateService promptTemplateService;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Lists the user's saved prompt templates, optionally filtered by a keyword
            matched against the template name. Use this to find a template's id before
            calling generateReply or composeFromScratch with that id. Returns each
            match's id, name, and category - not the full template text.""")
    public String listTemplates(@ToolParam(description = "Keyword to filter template names by", required = false) String keyword) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            PageResponse<PromptTemplateResponse> page = promptTemplateService.getTemplates(
                    ctx.userId(), ctx.organizationId(), PageRequest.of(0, FETCH_SIZE));

            String needle = keyword == null ? "" : keyword.trim().toLowerCase();
            String summary = page.content().stream()
                    .filter(t -> needle.isEmpty() || t.name().toLowerCase().contains(needle))
                    .limit(MAX_RESULTS)
                    .map(t -> "[" + t.id() + "] " + t.name() + " (" + t.category() + ")")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("No saved templates found" + (needle.isEmpty() ? "." : " matching '" + keyword + "'."));

            stepRecorder.record(TOOL_NAME, keyword, summary, AgentStepStatus.SUCCESS);
            return summary;
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, keyword, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "listTemplates failed: " + ex.getMessage();
        }
    }
}
