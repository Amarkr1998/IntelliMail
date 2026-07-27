package com.intellimail.mail.dto.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        Set<String> roles,
        Instant createdAt,
        UUID organizationId,
        String organizationName,
        String orgRole
) {
}
