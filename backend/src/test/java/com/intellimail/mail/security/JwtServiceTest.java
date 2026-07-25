package com.intellimail.mail.security;

import com.intellimail.mail.config.JwtProperties;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "unit-test-secret-key-must-be-at-least-256-bits-long-for-hs256",
                900_000L,
                604_800_000L,
                "intellimail-test"
        );
        jwtService = new JwtService(properties);

        User user = User.builder()
                .fullName("Test User")
                .email("test@intellimail.com")
                .password("hashed")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_USER).build()))
                .build();
        user.setId(UUID.randomUUID());
        principal = UserPrincipal.of(user);
    }

    @Test
    void generateAccessToken_isValidatedSuccessfullyAsAccessType() {
        String token = jwtService.generateAccessToken(principal);

        Claims claims = jwtService.validateToken(token, JwtService.TOKEN_TYPE_ACCESS);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(principal.getId());
        assertThat(claims.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("test@intellimail.com");
    }

    @Test
    void generateRefreshToken_isRejected_whenValidatedAsAccessType() {
        String refreshToken = jwtService.generateRefreshToken(principal);

        assertThatThrownBy(() -> jwtService.validateToken(refreshToken, JwtService.TOKEN_TYPE_ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_rejectsTamperedSignature() {
        String token = jwtService.generateAccessToken(principal);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.validateToken(tampered, JwtService.TOKEN_TYPE_ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }
}
