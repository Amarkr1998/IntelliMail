package com.intellimail.mail.security;

import com.intellimail.mail.enums.SubscriptionStatus;
import com.intellimail.mail.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SpEL-usable gate for AI-invoking endpoints, e.g.
 * {@code @PreAuthorize("@subscriptionGuard.hasActiveAccess(authentication)")}.
 * Applied only to endpoints that actually call the AI (email generation,
 * voice prompts) - never to read-only endpoints (history, templates listing,
 * billing itself), so a lapsed-trial org member can still see their data and
 * go upgrade.
 *
 * <p>Unlike {@link OrgSecurity}, this <em>does</em> need a DB read: unlike
 * {@code orgRole}, subscription status can change independent of the
 * {@code User} row at any moment via an async Stripe webhook, so it can't be
 * sourced from the already-fresh {@link UserPrincipal} the way org role is.
 */
@Component("subscriptionGuard")
@RequiredArgsConstructor
public class SubscriptionGuard {

    private final SubscriptionRepository subscriptionRepository;

    public boolean hasActiveAccess(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        // Solo (org-less) users are unaffected by billing entirely - v1 billing
        // only ever applies to organizations.
        if (principal.getOrganizationId() == null) {
            return true;
        }

        return subscriptionRepository.findByOrganizationId(principal.getOrganizationId())
                .map(subscription -> switch (subscription.getStatus()) {
                    case ACTIVE -> true;
                    case TRIALING -> subscription.getTrialEndsAt() != null && subscription.getTrialEndsAt().isAfter(Instant.now());
                    case PAST_DUE, CANCELED -> false;
                })
                // A missing row here would mean organization creation didn't
                // atomically create its Subscription - a bug in this app, not
                // a malicious caller. Fail open rather than lock out a
                // legitimate customer because of our own data-integrity bug.
                .orElse(true);
    }
}
