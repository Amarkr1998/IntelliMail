package com.intellimail.mail.service;

import com.intellimail.mail.dto.analytics.AnalyticsBreakdownItem;
import com.intellimail.mail.dto.analytics.AnalyticsResponse;
import com.intellimail.mail.mapper.AnalyticsMapper;
import com.intellimail.mail.repository.UsageAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UsageAnalyticsRepository usageAnalyticsRepository;
    private final AnalyticsMapper analyticsMapper;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UUID userId, Instant from, Instant to) {
        List<AnalyticsBreakdownItem> breakdown = usageAnalyticsRepository.summarizeByUser(userId, from, to).stream()
                .map(analyticsMapper::toBreakdownItem)
                .toList();

        long totalRequests = breakdown.stream().mapToLong(AnalyticsBreakdownItem::totalRequests).sum();
        long totalTokens = breakdown.stream().mapToLong(AnalyticsBreakdownItem::totalTokens).sum();
        double avgLatencyMs = breakdown.stream()
                .mapToDouble(AnalyticsBreakdownItem::avgLatencyMs)
                .average()
                .orElse(0.0);

        return new AnalyticsResponse(from, to, totalRequests, totalTokens, avgLatencyMs, breakdown);
    }
}
