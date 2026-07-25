package com.intellimail.mail.dto.email;

import com.intellimail.mail.enums.RequestType;

import java.time.Instant;
import java.util.UUID;

/** Returned by every /api/email/* generation endpoint and embedded in history. */
public record EmailReplyResponse(
        UUID id,
        UUID emailRequestId,
        RequestType requestType,
        String content,
        String aiModel,
        int attemptNumber,
        Integer totalTokens,
        Long latencyMs,
        boolean favorite,
        Instant createdAt
) {
}
