package com.intellimail.mail.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Powers POST /api/email/subject — generates subject line candidates for a given body. */
public record EmailSubjectRequest(

        @NotBlank(message = "Email content is required")
        @Size(max = 20_000, message = "Content must not exceed 20,000 characters")
        String content,

        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
}
