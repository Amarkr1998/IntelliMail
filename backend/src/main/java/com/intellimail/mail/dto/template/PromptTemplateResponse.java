package com.intellimail.mail.dto.template;

import com.intellimail.mail.enums.RequestType;

import java.time.Instant;
import java.util.UUID;

public record PromptTemplateResponse(
        UUID id,
        String name,
        String description,
        RequestType category,
        String promptText,
        String systemPrompt,
        boolean isPublic,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}
