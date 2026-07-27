package com.intellimail.mail.dto.organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String brandColor,
        Instant createdAt
) {
}
