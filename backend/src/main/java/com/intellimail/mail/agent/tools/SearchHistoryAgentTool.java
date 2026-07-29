package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.repository.EmailRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

/**
 * Read-only keyword search over the user's own past email requests - a
 * simple SQL LIKE search, not RAG/vector search. Its result feeds back into
 * the model's further reasoning, so it is not {@code returnDirect}.
 */
@Component
@RequiredArgsConstructor
public class SearchHistoryAgentTool {

    private static final String TOOL_NAME = "searchHistory";
    private static final int MAX_RESULTS = 5;
    private static final int SNIPPET_LENGTH = 200;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmailRequestRepository emailRequestRepository;
    private final AgentStepRecorder stepRecorder;

    @Tool(returnDirect = false, description = """
            Searches the user's own past email requests for a keyword. Use this when the
            goal references a previous email or past request (e.g. "reply the same way I
            replied to Sarah last time"). Returns short date + snippet summaries, not
            full content.""")
    public String searchHistory(@ToolParam(description = "Keyword to search for in past email content") String keyword) {
        AgentExecutionContext.Context ctx = AgentExecutionContext.current();
        try {
            Page<EmailRequest> matches = emailRequestRepository
                    .findByUserIdAndOriginalContentContainingIgnoreCaseOrderByCreatedAtDesc(
                            ctx.userId(), keyword, PageRequest.of(0, MAX_RESULTS));

            String summary = matches.isEmpty()
                    ? "No past emails found matching '" + keyword + "'."
                    : matches.getContent().stream()
                            .map(this::summarize)
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("No past emails found matching '" + keyword + "'.");

            stepRecorder.record(TOOL_NAME, keyword, summary, AgentStepStatus.SUCCESS);
            return summary;
        } catch (RuntimeException ex) {
            stepRecorder.record(TOOL_NAME, keyword, "ERROR: " + ex.getMessage(), AgentStepStatus.FAILED);
            return "searchHistory failed: " + ex.getMessage();
        }
    }

    private String summarize(EmailRequest request) {
        String content = request.getOriginalContent();
        String snippet = content.length() > SNIPPET_LENGTH ? content.substring(0, SNIPPET_LENGTH) + "..." : content;
        String date = request.getCreatedAt().atZone(ZoneOffset.UTC).format(DATE_FORMAT);
        return "[" + date + ", " + request.getRequestType() + "] " + snippet;
    }
}
