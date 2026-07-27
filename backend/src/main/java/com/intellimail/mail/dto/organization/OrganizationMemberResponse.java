package com.intellimail.mail.dto.organization;

import java.util.UUID;

public record OrganizationMemberResponse(
        UUID id,
        String fullName,
        String email,
        String orgRole
) {
}
