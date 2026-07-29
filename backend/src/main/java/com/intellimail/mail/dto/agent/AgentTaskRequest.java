package com.intellimail.mail.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Powers POST /api/agent/tasks. {@code context} stays a separate field from
 * {@code goal} (mirroring EmailGenerateRequest's originalContent/instructions
 * split) rather than requiring the email text to be embedded inline in the
 * goal text. Omit {@code conversationId} to start a new conversation; pass
 * back the one returned from a prior task to continue it.
 */
public record AgentTaskRequest(

        @NotBlank(message = "Goal is required")
        @Size(max = 4_000, message = "Goal must not exceed 4,000 characters")
        String goal,

        @Size(max = 20_000, message = "Context must not exceed 20,000 characters")
        String context,

        UUID conversationId,

        /** Background text extracted from an uploaded attachment (POST /api/email/extract) - never the goal/context text itself. */
        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
}
