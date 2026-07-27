package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code intellimail.app.*} properties — general application-level
 * config not specific to any one feature module.
 */
@ConfigurationProperties(prefix = "intellimail.app")
public record AppProperties(
        String frontendUrl
) {
}
