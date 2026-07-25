package com.intellimail.mail.logging;

import com.intellimail.mail.entity.AuditLog;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Central write-path for {@link AuditLog} rows, used across security,
 * email-generation and template flows.
 *
 * <p>Deliberately participates in the caller's existing transaction rather
 * than running in {@code Propagation.REQUIRES_NEW}: several call sites (e.g.
 * {@code AuthService.register}) audit a {@link User} row created earlier in
 * that same, still-uncommitted transaction. A REQUIRES_NEW audit write would
 * run in an independent transaction that cannot yet see that uncommitted
 * row, and fails the FK constraint on {@code audit_logs.user_id} — this was
 * caught by {@code AuthenticationFlowIntegrationTest} against a real
 * transactional database, not by the Mockito-based unit tests, which mock
 * the repository and never exercise real commit ordering.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRecorder {

    private final AuditLogRepository auditLogRepository;

    public void record(User user, String action, String entityType, String entityId, String details, HttpServletRequest request) {
        AuditLog entry = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(extractIp(request))
                .build();

        auditLogRepository.save(entry);
        log.debug("Audit event recorded: action={}, entityType={}, entityId={}", action, entityType, entityId);
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
