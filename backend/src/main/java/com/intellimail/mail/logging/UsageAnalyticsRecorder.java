package com.intellimail.mail.logging;

import com.intellimail.mail.entity.UsageAnalytics;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.repository.UsageAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central write-path for {@link UsageAnalytics} rows, backing the Analytics
 * dashboard (Module 8). Success is recorded in the caller's existing
 * transaction so it commits atomically with the {@code EmailRequest}/
 * {@code GeneratedReply} it describes; failure is recorded in its own
 * {@code REQUIRES_NEW} transaction (same reasoning as {@link AuditLogRecorder})
 * so the failure record survives the caller rolling back after an AI error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsageAnalyticsRecorder {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final UsageAnalyticsRepository usageAnalyticsRepository;

    public void recordSuccess(User user, RequestType requestType, Integer tokensUsed, Long latencyMs) {
        save(user, requestType, tokensUsed, latencyMs, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, RequestType requestType, String errorMessage) {
        save(user, requestType, null, null, false, errorMessage);
    }

    private void save(User user, RequestType requestType, Integer tokensUsed, Long latencyMs, boolean success, String errorMessage) {
        String truncatedError = errorMessage != null && errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH
                ? errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH)
                : errorMessage;

        usageAnalyticsRepository.save(UsageAnalytics.builder()
                .user(user)
                .requestType(requestType)
                .tokensUsed(tokensUsed)
                .latencyMs(latencyMs)
                .success(success)
                .errorMessage(truncatedError)
                .build());

        log.debug("Usage analytics recorded: user={}, requestType={}, success={}", user.getId(), requestType, success);
    }
}
