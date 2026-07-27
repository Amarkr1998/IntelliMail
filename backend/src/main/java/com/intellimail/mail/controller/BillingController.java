package com.intellimail.mail.controller;

import com.intellimail.mail.config.StripeProperties;
import com.intellimail.mail.dto.billing.CheckoutSessionResponse;
import com.intellimail.mail.dto.billing.CreateCheckoutSessionRequest;
import com.intellimail.mail.dto.billing.PortalSessionResponse;
import com.intellimail.mail.dto.billing.SubscriptionResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.BillingService;
import com.intellimail.mail.util.ApiResponse;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing", description = "Stripe-backed subscription billing - flat-rate tiers, hosted Checkout/Portal")
public class BillingController {

    private final BillingService billingService;
    private final StripeProperties stripeProperties;

    @GetMapping("/subscription")
    @Operation(summary = "Get the caller's organization subscription status")
    public ApiResponse<SubscriptionResponse> getSubscription(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(billingService.getSubscription(principal.getId()));
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("@orgSecurity.isOwner(authentication)")
    @Operation(summary = "Start a Stripe Checkout session", description = "OWNER only. Returns a redirect URL to Stripe's hosted checkout page.")
    public ApiResponse<CheckoutSessionResponse> createCheckoutSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return ApiResponse.success(billingService.createCheckoutSession(principal.getId(), request));
    }

    @PostMapping("/portal-session")
    @PreAuthorize("@orgSecurity.isOwner(authentication)")
    @Operation(summary = "Start a Stripe Customer Portal session", description = "OWNER only. Returns a redirect URL to Stripe's hosted billing portal.")
    public ApiResponse<PortalSessionResponse> createPortalSession(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(billingService.createPortalSession(principal.getId()));
    }

    /**
     * Public (see {@code SecurityConfig.PUBLIC_ENDPOINTS}) - Stripe can't
     * present a JWT. Authenticity comes entirely from the HMAC signature
     * check below, not from Spring Security.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook receiver", description = "Signature-verified via the Stripe-Signature header. Not for direct client use.")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook with invalid signature");
            return ResponseEntity.badRequest().build();
        }

        billingService.handleWebhookEvent(event);
        return ResponseEntity.ok().build();
    }
}
