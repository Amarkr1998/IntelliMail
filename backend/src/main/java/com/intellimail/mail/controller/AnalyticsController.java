package com.intellimail.mail.controller;

import com.intellimail.mail.dto.analytics.AnalyticsResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.AnalyticsService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Per-user AI usage analytics")
public class AnalyticsController {

    private static final long DEFAULT_WINDOW_DAYS = 30;

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Get usage analytics", description = "Aggregates request counts, token usage, and latency by request type. Defaults to the last 30 days.")
    public ApiResponse<AnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS);
        return ApiResponse.success(analyticsService.getAnalytics(principal.getId(), effectiveFrom, effectiveTo));
    }
}
