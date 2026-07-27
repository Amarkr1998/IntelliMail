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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * A full peer to {@link AuthService} rather than folded into it, for the same
 * reason {@code PasswordResetService} is separate: this needs a
 * {@link JwtDecoder} collaborator (Google's ID-token verifier) nothing else
 * touches. Token issuance itself is delegated back to
 * {@link AuthService#issueTokensFor(User)} - no duplicated JWT-minting logic.
 *
 * <p>Distinct from - and unrelated to - the Gmail-inbox-access OAuth flow
 * that was explored and dropped earlier: this is a much simpler ID-token
 * verification (no authorization-code exchange, no refresh tokens, no Gmail
 * API scopes), used purely as an alternative login method for the app's own
 * account system.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final Set<String> VALID_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder googleJwtDecoder;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogRecorder auditLogRecorder;

    @Transactional
    public AuthResponse loginOrRegister(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        Jwt jwt = decodeAndVerify(request.idToken());

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String fullName = jwt.getClaimAsString("name");
        if (fullName == null || fullName.isBlank()) {
            fullName = email;
        }

        User user = resolveUser(subject, email, fullName, httpRequest);

        auditLogRecorder.record(user, "GOOGLE_LOGIN", "User", user.getId().toString(), null, httpRequest);
        return authService.issueTokensFor(user);
    }

    private Jwt decodeAndVerify(String idToken) {
        Jwt jwt;
        try {
            jwt = googleJwtDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new GoogleTokenVerificationException("Invalid Google ID token", e);
        }

        if (!googleOAuthProperties.clientId().equals(jwt.getClaimAsString("aud"))) {
            throw new GoogleTokenVerificationException("Token audience does not match this application");
        }
        if (!VALID_ISSUERS.contains(jwt.getClaimAsString("iss"))) {
            throw new GoogleTokenVerificationException("Unexpected token issuer");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new GoogleTokenVerificationException("Google account email is not verified");
        }
        return jwt;
    }

    private User resolveUser(String subject, String email, String fullName, HttpServletRequest httpRequest) {
        Optional<User> byGoogleId = userRepository.findByGoogleSubjectId(subject);
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleSubjectId(subject);
            return userRepository.save(existing);
        }

        return registerFromGoogle(subject, email, fullName, httpRequest);
    }

    private User registerFromGoogle(String subject, String email, String fullName, HttpServletRequest httpRequest) {
        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role ROLE_USER is not seeded — check Flyway migration V9__seed_roles.sql"));

        // Never exposed to the user - this account only ever authenticates via
        // Google Sign-In, but the password column stays NOT NULL and every
        // existing password-based code path keeps working unmodified.
        String unusablePassword = Base64.getUrlEncoder().withoutPadding().encodeToString(KeyGenerators.secureRandom(32).generateKey());

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(unusablePassword))
                .googleSubjectId(subject)
                .roles(Set.of(defaultRole))
                .build();
        user = userRepository.save(user);

        auditLogRecorder.record(user, "USER_REGISTERED_VIA_GOOGLE", "User", user.getId().toString(), null, httpRequest);
        return user;
    }
}
