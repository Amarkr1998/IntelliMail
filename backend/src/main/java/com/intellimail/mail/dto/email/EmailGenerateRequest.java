package com.intellimail.mail.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Powers POST /api/email/generate — draft a reply to an existing email thread. */
public record EmailGenerateRequest(

        @NotBlank(message = "Original email content is required")
        @Size(max = 20_000, message = "Original content must not exceed 20,000 characters")
        String originalContent,

        @Size(max = 2_000, message = "Instructions must not exceed 2,000 characters")
        String instructions,

        UUID promptTemplateId
) {
    // Compact form kept simple; promptTemplateId is optional (null = no template).
}
