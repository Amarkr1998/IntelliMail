package com.intellimail.mail.dto.billing;

import com.intellimail.mail.enums.PlanId;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(
        @NotNull PlanId planId
) {
}
