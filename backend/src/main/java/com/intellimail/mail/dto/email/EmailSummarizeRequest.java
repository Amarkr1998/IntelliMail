package com.intellimail.mail.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Powers POST /api/email/summarize. */
public record EmailSummarizeRequest(

        @NotBlank(message = "Email content is required")
        @Size(max = 20_000, message = "Content must not exceed 20,000 characters")
        String content
) {
}
