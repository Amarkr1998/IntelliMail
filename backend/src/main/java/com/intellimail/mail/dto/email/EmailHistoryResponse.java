package com.intellimail.mail.dto.email;

import com.intellimail.mail.enums.RequestType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Returned by GET /api/history — one email request with all of its generated reply attempts. */
public record EmailHistoryResponse(
        UUID id,
        RequestType requestType,
        String originalContent,
        Instant createdAt,
        List<EmailReplyResponse> replies
) {
}
