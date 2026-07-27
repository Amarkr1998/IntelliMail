package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code intellimail.google-oauth.*} - the client id used to verify Google Sign-In ID tokens' {@code aud} claim. */
@ConfigurationProperties(prefix = "intellimail.google-oauth")
public record GoogleOAuthProperties(
        String clientId
) {
}
