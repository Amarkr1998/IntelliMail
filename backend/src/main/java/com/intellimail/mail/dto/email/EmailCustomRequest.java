package com.intellimail.mail.dto.email;

import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.validation.ValidCustomRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Powers POST /api/email/custom — the generic "compose from scratch" endpoint
 * backing the Meeting Request, Thank You, Apology, Sales, HR, Marketing,
 * Cold Outreach and fully-Custom-Prompt generators. {@code requestType}
 * selects which system prompt (Module 6) is used; {@code customPrompt} lets
 * the user layer additional free-form instructions on top of it.
 */
public record EmailCustomRequest(

        @NotNull(message = "Request type is required")
        @ValidCustomRequestType
        RequestType requestType,

        @NotBlank(message = "Context describing the email to generate is required")
        @Size(max = 10_000, message = "Context must not exceed 10,000 characters")
        String context,

        @Size(max = 5_000, message = "Custom prompt must not exceed 5,000 characters")
        String customPrompt,

        UUID promptTemplateId
) {
}
