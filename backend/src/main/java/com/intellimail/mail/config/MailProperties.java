package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code intellimail.mail.*} properties used when composing outgoing
 * mail (as opposed to {@code spring.mail.*}, which configures the SMTP
 * connection itself).
 */
@ConfigurationProperties(prefix = "intellimail.mail")
public record MailProperties(
        String fromAddress
) {
}
