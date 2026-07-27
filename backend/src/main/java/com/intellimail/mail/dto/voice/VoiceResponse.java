package com.intellimail.mail.dto.voice;

import java.time.Instant;
import java.util.UUID;

/** Returned by POST /api/voice/prompt and embedded in GET /api/voice/history. */
public record VoiceResponse(
        UUID id,
        String transcript,
        String aiResponse,
        String language,
        String aiModel,
        Integer totalTokens,
        Long latencyMs,
        Instant createdAt
) {
}
