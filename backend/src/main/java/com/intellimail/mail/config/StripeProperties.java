package com.intellimail.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code intellimail.stripe.*}. Only two plans exist in v1 (flat-rate, no usage-based billing). */
@ConfigurationProperties(prefix = "intellimail.stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String priceIdStarter,
        String priceIdPro
) {
}
