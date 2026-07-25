package com.intellimail.mail.dto.email;

import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.validation.ValidRewriteStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Powers POST /api/email/improve. {@code style} must be one of the rewrite
 * types (PROFESSIONAL_REWRITE, FRIENDLY_REWRITE, FORMAL_REWRITE,
 * CASUAL_REWRITE, GRAMMAR_CORRECTION, EXPAND, SHORTEN) — enforced by
 * {@code @ValidRewriteStyle}.
 */
public record EmailImproveRequest(

        @NotBlank(message = "Email content is required")
        @Size(max = 20_000, message = "Content must not exceed 20,000 characters")
        String content,

        @NotNull(message = "Rewrite style is required")
        @ValidRewriteStyle
        RequestType style,

        @Size(max = 20_000, message = "Reference context must not exceed 20,000 characters")
        String referenceContext
) {
}
