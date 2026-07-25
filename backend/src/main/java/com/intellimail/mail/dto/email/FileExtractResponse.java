package com.intellimail.mail.dto.email;

/**
 * Returned by POST /api/email/extract. {@code content} is meant to seed the
 * {@code originalContent}/{@code content}/{@code context} field of any other
 * /api/email/* request — extraction never triggers an AI call or persists
 * anything itself, so no {@code EmailRequest} exists for it until the caller
 * subsequently invokes an actual generation endpoint with this text.
 */
public record FileExtractResponse(
        String fileName,
        String content,
        int characterCount,
        boolean truncated
) {
}
