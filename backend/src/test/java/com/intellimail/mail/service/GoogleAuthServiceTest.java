package com.intellimail.mail.service;

import com.intellimail.mail.config.GoogleOAuthProperties;
import com.intellimail.mail.dto.auth.AuthResponse;
import com.intellimail.mail.dto.auth.GoogleLoginRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.exception.GoogleTokenVerificationException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.RoleRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";

    @Mock
    private JwtDecoder googleJwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;
    @Mock
    private AuditLogRecorder auditLogRecorder;
    @Mock
    private HttpServletRequest httpServletRequest;

    private final GoogleOAuthProperties googleOAuthProperties = new GoogleOAuthProperties(CLIENT_ID);

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthService(
                googleJwtDecoder, googleOAuthProperties, userRepository, roleRepository, passwordEncoder, authService, auditLogRecorder);
    }

    private Jwt validJwt(String subject, String email) {
        return Jwt.withTokenValue("raw-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("aud", CLIENT_ID)
                .claim("iss", "https://accounts.google.com")
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", "Test User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void loginOrRegister_newUser_registersAndIssuesTokens() {
        when(googleJwtDecoder.decode("raw-token")).thenReturn(validJwt("google-sub-1", "newuser@intellimail.com"));
        when(userRepository.findByGoogleSubjectId("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@intellimail.com")).thenReturn(Optional.empty());
        Role userRole = Role.builder().name(RoleName.ROLE_USER).build();
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        AuthResponse expectedResponse = AuthResponse.of("access", "refresh", 900_000L, null);
        when(authService.issueTokensFor(any(User.class))).thenReturn(expectedResponse);

        AuthResponse response = googleAuthService.loginOrRegister(new GoogleLoginRequest("raw-token"), httpServletRequest);

        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("newuser@intellimail.com");
        assertThat(captor.getValue().getGoogleSubjectId()).isEqualTo("google-sub-1");
        verify(auditLogRecorder).record(any(User.class), eq("USER_REGISTERED_VIA_GOOGLE"), eq("User"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void loginOrRegister_existingUserByGoogleId_reusesAccountWithoutRegistering() {
        User existing = User.builder().fullName("Existing").email("existing@intellimail.com").password("hashed").build();
        existing.setId(UUID.randomUUID());
        existing.setGoogleSubjectId("google-sub-2");

        when(googleJwtDecoder.decode("raw-token")).thenReturn(validJwt("google-sub-2", "existing@intellimail.com"));
        when(userRepository.findByGoogleSubjectId("google-sub-2")).thenReturn(Optional.of(existing));
        when(authService.issueTokensFor(existing)).thenReturn(AuthResponse.of("a", "r", 1, null));

        googleAuthService.loginOrRegister(new GoogleLoginRequest("raw-token"), httpServletRequest);

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void loginOrRegister_existingUserByEmail_linksGoogleSubjectId() {
        User existing = User.builder().fullName("Existing").email("existing@intellimail.com").password("hashed").build();
        existing.setId(UUID.randomUUID());

        when(googleJwtDecoder.decode("raw-token")).thenReturn(validJwt("google-sub-3", "existing@intellimail.com"));
        when(userRepository.findByGoogleSubjectId("google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@intellimail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(authService.issueTokensFor(existing)).thenReturn(AuthResponse.of("a", "r", 1, null));

        googleAuthService.loginOrRegister(new GoogleLoginRequest("raw-token"), httpServletRequest);

        assertThat(existing.getGoogleSubjectId()).isEqualTo("google-sub-3");
        verify(userRepository).save(existing);
    }

    @Test
    void loginOrRegister_throwsGoogleTokenVerification_whenAudienceMismatch() {
        Jwt jwt = Jwt.withTokenValue("raw-token")
                .header("alg", "RS256")
                .subject("google-sub-4")
                .claim("aud", "some-other-client-id")
                .claim("iss", "https://accounts.google.com")
                .claim("email", "x@intellimail.com")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(googleJwtDecoder.decode("raw-token")).thenReturn(jwt);

        assertThatThrownBy(() -> googleAuthService.loginOrRegister(new GoogleLoginRequest("raw-token"), httpServletRequest))
                .isInstanceOf(GoogleTokenVerificationException.class);
    }

    @Test
    void loginOrRegister_throwsGoogleTokenVerification_whenEmailNotVerified() {
        Jwt jwt = Jwt.withTokenValue("raw-token")
                .header("alg", "RS256")
                .subject("google-sub-5")
                .claim("aud", CLIENT_ID)
                .claim("iss", "https://accounts.google.com")
                .claim("email", "x@intellimail.com")
                .claim("email_verified", false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(googleJwtDecoder.decode("raw-token")).thenReturn(jwt);

        assertThatThrownBy(() -> googleAuthService.loginOrRegister(new GoogleLoginRequest("raw-token"), httpServletRequest))
                .isInstanceOf(GoogleTokenVerificationException.class);
    }

    @Test
    void loginOrRegister_throwsGoogleTokenVerification_whenDecodeFails() {
        when(googleJwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("bad signature"));

        assertThatThrownBy(() -> googleAuthService.loginOrRegister(new GoogleLoginRequest("bad-token"), httpServletRequest))
                .isInstanceOf(GoogleTokenVerificationException.class);
    }
}
