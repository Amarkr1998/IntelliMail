package com.intellimail.mail.dto.agent;

public record AgentStepResponse(
        int stepNumber,
        String toolName,
        String inputSummary,
        String outputSummary,
        String status
) {
}
