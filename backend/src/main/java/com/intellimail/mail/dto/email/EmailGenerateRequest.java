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

        UUID promptTemplateId,

        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
    // Compact form kept simple; promptTemplateId/referenceContext are optional.
    // referenceContext is background information (e.g. text extracted from an
    // uploaded file via POST /api/email/extract) - never the email being replied
    // to itself, which stays in originalContent.
}
