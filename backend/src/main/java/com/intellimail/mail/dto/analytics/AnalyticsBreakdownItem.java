package com.intellimail.mail.dto.analytics;

import com.intellimail.mail.enums.RequestType;

public record AnalyticsBreakdownItem(
        RequestType requestType,
        long totalRequests,
        long totalTokens,
        double avgLatencyMs
) {
}
