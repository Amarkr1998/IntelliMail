package com.intellimail.mail.service;

import com.intellimail.mail.config.AppProperties;
import com.intellimail.mail.dto.auth.ForgotPasswordRequest;
import com.intellimail.mail.dto.auth.ResetPasswordRequest;
import com.intellimail.mail.entity.PasswordResetToken;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.exception.InvalidTokenException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.PasswordResetTokenRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * A full peer to {@link AuthService} rather than a method added there: the
 * only concern the two share is {@code User.password}, but this feature needs
 * two collaborators ({@link PasswordResetTokenRepository}, {@link MailService})
 * the other three {@code AuthService} methods (register/login/refresh) never
 * touch, so it gets its own service with its own direct dependencies rather
 * than calling back into {@code AuthService}.
 *
 * <p>Deliberate, documented gaps for v1 (see PR description, not bugs):
 * <ul>
 *   <li>No scheduled cleanup of expired/used token rows - no {@code @Scheduled}
 *       infrastructure exists anywhere in this app yet; disproportionate to add
 *       just for housekeeping on a small, slow-growing table.</li>
 *   <li>No JWT/session revocation on reset - this app's JWTs are stateless with
 *       no server-side blacklist at all today; a much larger pre-existing gap,
 *       not something this feature should silently take on.</li>
 *   <li>{@code forgotPassword}'s response time itself is a timing side-channel
 *       (the existing-email path does a synchronous SMTP send before
 *       returning; the non-existent-email path returns immediately) - left
 *       unfixed deliberately, with direct precedent already in this codebase:
 *       {@code AuthService.login()} has the same characteristic today via
 *       BCrypt's deliberately-slow {@code matches()} only running on the
 *       known-email path.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long COOLDOWN_SECONDS = 60;
    private static final long TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRecorder auditLogRecorder;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final AppProperties appProperties;

    /**
     * Always returns normally regardless of whether the email exists, a
     * cooldown silently suppressed sending, or the mail send itself failed -
     * the caller (and thus the HTTP client) can never distinguish these cases,
     * which is what makes the enumeration protection actually hold.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        userRepository.findByEmail(request.email()).ifPresent(user -> issueResetToken(user, httpRequest));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String tokenHash = hash(request.token());
        Instant now = Instant.now();

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset link"));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Consumes this token and closes any other still-outstanding link for
        // this user (e.g. an earlier, unread "forgot password" email).
        tokenRepository.invalidateAllUnusedForUser(user.getId(), now);

        auditLogRecorder.record(user, "PASSWORD_RESET_COMPLETED", "User", user.getId().toString(), null, httpRequest);
    }

    private void issueResetToken(User user, HttpServletRequest httpRequest) {
        Instant now = Instant.now();

        Optional<PasswordResetToken> recent =
                tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId());
        if (recent.isPresent() && recent.get().getCreatedAt().isAfter(now.minus(COOLDOWN_SECONDS, ChronoUnit.SECONDS))) {
            return;
        }

        // Close the window where multiple valid reset links could coexist.
        tokenRepository.invalidateAllUnusedForUser(user.getId(), now);

        String rawToken = generateRawToken();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                .build());

        String resetLink = appProperties.frontendUrl() + "/reset-password?token=" + rawToken;
        try {
            mailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        } catch (MailException ex) {
            // Never log the raw email address; the user id is enough to
            // correlate in the audit log / DB if follow-up is needed. Swallow
            // rather than rethrow - forgotPassword() must still return
            // normally either way, see class Javadoc.
            log.error("Failed to send password reset email for user {}", user.getId(), ex);
            return;
        }

        auditLogRecorder.record(user, "PASSWORD_RESET_REQUESTED", "User", user.getId().toString(), null, httpRequest);
    }

    private String generateRawToken() {
        byte[] bytes = KeyGenerators.secureRandom(TOKEN_BYTES).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JDK; this can't happen.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
