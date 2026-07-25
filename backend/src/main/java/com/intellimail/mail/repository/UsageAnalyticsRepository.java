package com.intellimail.mail.repository;

import com.intellimail.mail.entity.UsageAnalytics;
import com.intellimail.mail.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UsageAnalyticsRepository extends JpaRepository<UsageAnalytics, UUID> {

    /** Per-request-type usage counts and token totals for a user within a window, for the Analytics dashboard. */
    @Query("""
            SELECT ua.requestType AS requestType,
                   COUNT(ua) AS totalRequests,
                   COALESCE(SUM(ua.tokensUsed), 0) AS totalTokens,
                   COALESCE(AVG(ua.latencyMs), 0) AS avgLatencyMs
            FROM UsageAnalytics ua
            WHERE ua.user.id = :userId AND ua.createdAt BETWEEN :from AND :to
            GROUP BY ua.requestType
            """)
    List<UsageSummaryRow> summarizeByUser(@Param("userId") UUID userId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);

    interface UsageSummaryRow {
        RequestType getRequestType();

        long getTotalRequests();

        long getTotalTokens();

        double getAvgLatencyMs();
    }
}
