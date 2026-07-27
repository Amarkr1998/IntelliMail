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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogRecorder auditLogRecorder;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private MailService mailService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private final AppProperties appProperties = new AppProperties("http://localhost:5173");

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository, passwordEncoder, auditLogRecorder, tokenRepository, mailService, appProperties);
    }

    private User user() {
        User user = User.builder()
                .fullName("Ada Lovelace")
                .email("ada@intellimail.com")
                .password("encoded")
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void forgotPassword_withUnknownEmail_returnsNormally_andSendsNoMail() {
        when(userRepository.findByEmail("nobody@intellimail.com")).thenReturn(Optional.empty());

        passwordResetService.forgotPassword(new ForgotPasswordRequest("nobody@intellimail.com"), httpServletRequest);

        verifyNoInteractions(mailService, tokenRepository, auditLogRecorder);
    }

    @Test
    void forgotPassword_happyPath_savesTokenAndSendsEmail() {
        User user = user();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()), httpServletRequest);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getTokenHash()).hasSize(64);

        verify(mailService).sendPasswordResetEmail(eq(user.getEmail()), eq(user.getFullName()), anyString());
        verify(tokenRepository).invalidateAllUnusedForUser(eq(user.getId()), any(Instant.class));
        verify(auditLogRecorder).record(eq(user), eq("PASSWORD_RESET_REQUESTED"), eq("User"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void forgotPassword_withinCooldown_skipsIssuingAnotherToken() {
        User user = user();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        PasswordResetToken recent = PasswordResetToken.builder().user(user).tokenHash("h").expiresAt(Instant.now()).build();
        recent.setCreatedAt(Instant.now().minus(10, ChronoUnit.SECONDS));
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(recent));

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()), httpServletRequest);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(mailService, auditLogRecorder);
    }

    @Test
    void forgotPassword_whenMailSendFails_stillReturnsNormally() {
        User user = user();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());
        doThrow(new MailSendException("smtp down")).when(mailService)
                .sendPasswordResetEmail(anyString(), anyString(), anyString());

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()), httpServletRequest);

        verify(auditLogRecorder, never()).record(any(), eq("PASSWORD_RESET_REQUESTED"), any(), any(), any(), any());
    }

    @Test
    void resetPassword_withExpiredToken_throwsInvalidToken() {
        User user = user();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("raw-token", "newPassword123"), httpServletRequest))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_withAlreadyUsedToken_throwsInvalidToken() {
        User user = user();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .usedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();
        when(tokenRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("raw-token", "newPassword123"), httpServletRequest))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_withUnknownToken_throwsInvalidToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("bogus", "newPassword123"), httpServletRequest))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_happyPath_encodesPasswordAndInvalidatesOtherTokens() {
        User user = user();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded");

        passwordResetService.resetPassword(new ResetPasswordRequest("raw-token", "newPassword123"), httpServletRequest);

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(user);
        verify(tokenRepository, times(1)).invalidateAllUnusedForUser(eq(user.getId()), any(Instant.class));
        verify(auditLogRecorder).record(eq(user), eq("PASSWORD_RESET_COMPLETED"), eq("User"), any(), any(), eq(httpServletRequest));
    }

    private static String sha256(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
