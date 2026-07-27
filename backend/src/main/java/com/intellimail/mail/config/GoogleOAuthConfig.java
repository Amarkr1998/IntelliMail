package com.intellimail.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * A single {@link NimbusJwtDecoder}, built once so its internal JWKS cache
 * (with Nimbus's own refresh policy) is shared across requests rather than
 * re-fetched on every Google Sign-In. This decoder only ever verifies tokens
 * handed to us by the frontend - it is not part of the servlet filter chain,
 * unlike a resource-server {@code JwtDecoder} normally would be.
 */
@Configuration
public class GoogleOAuthConfig {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    @Bean
    public JwtDecoder googleJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
    }
}
