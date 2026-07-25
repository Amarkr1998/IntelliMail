package com.intellimail.mail.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Powers POST /api/email/followup — drafts a follow-up for a thread that received no response. */
public record EmailFollowupRequest(

        @NotBlank(message = "Original email content is required")
        @Size(max = 20_000, message = "Content must not exceed 20,000 characters")
        String originalContent,

        @Size(max = 2_000, message = "Instructions must not exceed 2,000 characters")
        String instructions,

        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
}
