package com.intellimail.mail.agent;

import com.intellimail.mail.enums.AgentStepStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the trail of tool calls an agent task made, since {@code
 * ChatClient.call()} itself exposes no "which tools ran" API - each tool
 * records its own step here as its last action, and {@code AgentOrchestrator}
 * drains and persists them once the top-level call returns.
 */
@Component
public class AgentStepRecorder {

    private static final int SUMMARY_MAX_LENGTH = 500;

    public record RecordedStep(String toolName, String inputSummary, String outputSummary, AgentStepStatus status) {
    }

    private static final ThreadLocal<List<RecordedStep>> STEPS = ThreadLocal.withInitial(ArrayList::new);

    public void record(String toolName, String input, String output, AgentStepStatus status) {
        STEPS.get().add(new RecordedStep(toolName, truncate(input), truncate(output), status));
    }

    /** Returns the accumulated steps and clears them, so nothing leaks onto the next request on a pooled thread. */
    public List<RecordedStep> drain() {
        List<RecordedStep> steps = STEPS.get();
        STEPS.remove();
        return steps;
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > SUMMARY_MAX_LENGTH ? text.substring(0, SUMMARY_MAX_LENGTH) + "..." : text;
    }
}
