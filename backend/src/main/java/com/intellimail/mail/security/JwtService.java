package com.intellimail.mail.security;

import com.intellimail.mail.config.JwtProperties;
import com.intellimail.mail.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates the platform's two token types: short-lived access
 * tokens (used on every authenticated request) and longer-lived refresh
 * tokens (used only against POST /api/auth/refresh). Both are stateless
 * signed JWTs — no server-side session/refresh-token table is required.
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, TOKEN_TYPE_ACCESS, jwtProperties.accessTokenExpirationMs());
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, TOKEN_TYPE_REFRESH, jwtProperties.refreshTokenExpirationMs());
    }

    /** Parses and verifies signature/expiry, then asserts the token's {@code type} claim matches. */
    public Claims validateToken(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired token", ex);
        }

        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(actualType)) {
            throw new InvalidTokenException("Expected a '" + expectedType + "' token but got '" + actualType + "'");
        }
        return claims;
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    private String buildToken(UserPrincipal principal, String tokenType, long expirationMs) {
        Instant now = Instant.now();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim(CLAIM_EMAIL, principal.getUsername())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, tokenType)
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
