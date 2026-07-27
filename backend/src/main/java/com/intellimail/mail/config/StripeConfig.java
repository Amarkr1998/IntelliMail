package com.intellimail.mail.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The one place in this codebase where configuration isn't plain
 * dependency-injected: {@code stripe-java}'s API is inherently a static
 * field ({@link Stripe#apiKey}), not something the SDK offers a
 * Spring-idiomatic alternative to.
 */
@Component
@RequiredArgsConstructor
public class StripeConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    public void configureStripeApiKey() {
        Stripe.apiKey = stripeProperties.secretKey();
    }
}
