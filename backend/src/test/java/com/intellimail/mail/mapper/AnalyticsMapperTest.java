package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.analytics.AnalyticsBreakdownItem;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.repository.UsageAnalyticsRepository.UsageSummaryRow;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsMapperTest {

    private final AnalyticsMapper analyticsMapper = Mappers.getMapper(AnalyticsMapper.class);

    @Test
    void toBreakdownItem_mapsProjectionFieldsByName() {
        UsageSummaryRow row = new UsageSummaryRow() {
            @Override
            public RequestType getRequestType() {
                return RequestType.SUMMARIZE;
            }

            @Override
            public long getTotalRequests() {
                return 42L;
            }

            @Override
            public long getTotalTokens() {
                return 12_345L;
            }

            @Override
            public double getAvgLatencyMs() {
                return 850.5;
            }
        };

        AnalyticsBreakdownItem item = analyticsMapper.toBreakdownItem(row);

        assertThat(item.requestType()).isEqualTo(RequestType.SUMMARIZE);
        assertThat(item.totalRequests()).isEqualTo(42L);
        assertThat(item.totalTokens()).isEqualTo(12_345L);
        assertThat(item.avgLatencyMs()).isEqualTo(850.5);
    }
}
