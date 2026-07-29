package com.intellimail.mail.dto.agent;

import java.time.Instant;
import java.util.UUID;

public record AgentTaskSummaryResponse(
        UUID id,
        String goal,
        String status,
        UUID conversationId,
        Instant createdAt
) {
}
