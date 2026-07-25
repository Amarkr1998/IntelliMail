package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code intellimail.jwt.*} properties used by the token issuing
 * and validation components in the security module.
 */
@ConfigurationProperties(prefix = "intellimail.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs,
        String issuer
) {
}
