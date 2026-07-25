package com.intellimail.mail.service;

import com.intellimail.mail.dto.analytics.AnalyticsResponse;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.mapper.AnalyticsMapper;
import com.intellimail.mail.mapper.AnalyticsMapperImpl;
import com.intellimail.mail.repository.UsageAnalyticsRepository;
import com.intellimail.mail.repository.UsageAnalyticsRepository.UsageSummaryRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UsageAnalyticsRepository usageAnalyticsRepository;

    private final AnalyticsMapper analyticsMapper = new AnalyticsMapperImpl();

    private AnalyticsService analyticsService;
    private UUID userId;
    private Instant from;
    private Instant to;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(usageAnalyticsRepository, analyticsMapper);
        userId = UUID.randomUUID();
        to = Instant.now();
        from = to.minus(30, ChronoUnit.DAYS);
    }

    @Test
    void getAnalytics_aggregatesTotalsAcrossBreakdownRows() {
        when(usageAnalyticsRepository.summarizeByUser(any(), any(), any())).thenReturn(List.of(
                summaryRow(RequestType.GENERATE_REPLY, 10, 1000, 500.0),
                summaryRow(RequestType.SUMMARIZE, 5, 400, 300.0)
        ));

        AnalyticsResponse response = analyticsService.getAnalytics(userId, from, to);

        assertThat(response.totalRequests()).isEqualTo(15);
        assertThat(response.totalTokens()).isEqualTo(1400);
        assertThat(response.avgLatencyMs()).isEqualTo(400.0);
        assertThat(response.breakdown()).hasSize(2);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
    }

    @Test
    void getAnalytics_returnsZeroedResponse_whenNoUsageRecorded() {
        when(usageAnalyticsRepository.summarizeByUser(any(), any(), any())).thenReturn(List.of());

        AnalyticsResponse response = analyticsService.getAnalytics(userId, from, to);

        assertThat(response.totalRequests()).isZero();
        assertThat(response.totalTokens()).isZero();
        assertThat(response.avgLatencyMs()).isZero();
        assertThat(response.breakdown()).isEmpty();
    }

    private UsageSummaryRow summaryRow(RequestType requestType, long totalRequests, long totalTokens, double avgLatencyMs) {
        return new UsageSummaryRow() {
            @Override
            public RequestType getRequestType() {
                return requestType;
            }

            @Override
            public long getTotalRequests() {
                return totalRequests;
            }

            @Override
            public long getTotalTokens() {
                return totalTokens;
            }

            @Override
            public double getAvgLatencyMs() {
                return avgLatencyMs;
            }
        };
    }
}
