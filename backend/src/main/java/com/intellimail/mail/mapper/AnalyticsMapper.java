package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.analytics.AnalyticsBreakdownItem;
import com.intellimail.mail.repository.UsageAnalyticsRepository.UsageSummaryRow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnalyticsMapper {

    AnalyticsBreakdownItem toBreakdownItem(UsageSummaryRow row);
}
