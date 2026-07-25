package com.intellimail.mail.dto.analytics;

import java.time.Instant;
import java.util.List;

/** Returned by GET /api/analytics for the requested time window. */
public record AnalyticsResponse(
        Instant from,
        Instant to,
        long totalRequests,
        long totalTokens,
        double avgLatencyMs,
        List<AnalyticsBreakdownItem> breakdown
) {
}
