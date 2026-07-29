package com.intellimail.mail.dto.agent;

import java.util.List;
import java.util.UUID;

public record AgentTaskResponse(
        UUID taskId,
        String status,
        String finalResult,
        List<AgentStepResponse> steps,
        PendingActionResponse pendingAction,
        UUID conversationId
) {
}
