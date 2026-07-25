package com.intellimail.mail.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Powers POST /api/email/translate. */
public record EmailTranslateRequest(

        @NotBlank(message = "Email content is required")
        @Size(max = 20_000, message = "Content must not exceed 20,000 characters")
        String content,

        @NotBlank(message = "Target language is required")
        @Size(max = 40, message = "Target language must not exceed 40 characters")
        String targetLanguage,

        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
}
