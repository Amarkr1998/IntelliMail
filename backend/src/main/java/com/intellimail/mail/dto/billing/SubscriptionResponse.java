package com.intellimail.mail.dto.billing;

import java.time.Instant;

public record SubscriptionResponse(
        String planId,
        String status,
        Instant trialEndsAt,
        Instant currentPeriodEnd,
        boolean active
) {
}
