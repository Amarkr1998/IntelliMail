package com.intellimail.mail.service;

import com.intellimail.mail.config.AppProperties;
import com.intellimail.mail.config.StripeProperties;
import com.intellimail.mail.dto.billing.CheckoutSessionResponse;
import com.intellimail.mail.dto.billing.CreateCheckoutSessionRequest;
import com.intellimail.mail.dto.billing.PortalSessionResponse;
import com.intellimail.mail.dto.billing.SubscriptionResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.Subscription;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.PlanId;
import com.intellimail.mail.enums.SubscriptionStatus;
import com.intellimail.mail.exception.BillingException;
import com.intellimail.mail.exception.NoBillingAccountException;
import com.intellimail.mail.exception.UserNotFoundException;
import com.intellimail.mail.exception.UserNotInOrganizationException;
import com.intellimail.mail.repository.SubscriptionRepository;
import com.intellimail.mail.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Flat-rate subscription tiers only - no usage-based/metered billing (a
 * documented v1 scope decision). Stripe's hosted Checkout and Customer Portal
 * cover the actual payment UI/invoice history, so this service only ever
 * creates redirect sessions and reconciles webhook events; it never renders
 * or stores payment details itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private static final long TRIAL_DAYS = 14;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final StripeProperties stripeProperties;
    private final AppProperties appProperties;

    /** Called once, at organization-creation time (see {@code OrganizationService}). Never calls Stripe. */
    public Subscription createTrialSubscription(Organization organization) {
        return subscriptionRepository.save(Subscription.builder()
                .organization(organization)
                .planId(PlanId.FREE_TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialEndsAt(Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS))
                .build());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID userId) {
        Subscription subscription = requireSubscription(userId);
        return toResponse(subscription);
    }

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(UUID userId, CreateCheckoutSessionRequest request) {
        if (request.planId() == PlanId.FREE_TRIAL) {
            throw new IllegalArgumentException("Cannot check out into the free trial plan");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Subscription subscription = requireSubscription(userId);

        try {
            if (subscription.getStripeCustomerId() == null) {
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(subscription.getOrganization().getName())
                        .build();
                Customer customer = Customer.create(customerParams);
                subscription.setStripeCustomerId(customer.getId());
                subscriptionRepository.save(subscription);
            }

            var params = com.stripe.param.checkout.SessionCreateParams.builder()
                    .setMode(Mode.SUBSCRIPTION)
                    .setCustomer(subscription.getStripeCustomerId())
                    .setSuccessUrl(appProperties.frontendUrl() + "/billing?checkout=success")
                    .setCancelUrl(appProperties.frontendUrl() + "/billing?checkout=cancelled")
                    .addLineItem(LineItem.builder()
                            .setPrice(priceIdFor(request.planId()))
                            .setQuantity(1L)
                            .build())
                    .putMetadata("planId", request.planId().name())
                    .putMetadata("organizationId", subscription.getOrganization().getId().toString())
                    .build();

            Session session = Session.create(params);
            return new CheckoutSessionResponse(session.getUrl());
        } catch (StripeException e) {
            throw new BillingException("Failed to create Stripe checkout session", e);
        }
    }

    @Transactional(readOnly = true)
    public PortalSessionResponse createPortalSession(UUID userId) {
        Subscription subscription = requireSubscription(userId);
        if (subscription.getStripeCustomerId() == null) {
            throw new NoBillingAccountException("Start a subscription before managing billing");
        }

        try {
            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(
                    SessionCreateParams.builder()
                            .setCustomer(subscription.getStripeCustomerId())
                            .setReturnUrl(appProperties.frontendUrl() + "/billing")
                            .build());
            return new PortalSessionResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new BillingException("Failed to create Stripe billing portal session", e);
        }
    }

    /**
     * Always a full upsert keyed on the Stripe identifiers rather than an
     * incremental change - Stripe can and does redeliver webhook events, so
     * every handler here is naturally idempotent without extra dedup
     * bookkeeping.
     */
    @Transactional
    public void handleWebhookEvent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        switch (event.getType()) {
            case "checkout.session.completed" -> deserializer.getObject().ifPresent(obj -> {
                if (obj instanceof Session session) {
                    handleCheckoutCompleted(session);
                }
            });
            case "customer.subscription.updated" -> deserializer.getObject().ifPresent(obj -> {
                if (obj instanceof com.stripe.model.Subscription stripeSubscription) {
                    handleSubscriptionUpdated(stripeSubscription);
                }
            });
            case "customer.subscription.deleted" -> deserializer.getObject().ifPresent(obj -> {
                if (obj instanceof com.stripe.model.Subscription stripeSubscription) {
                    handleSubscriptionDeleted(stripeSubscription);
                }
            });
            default -> log.debug("Ignoring unhandled Stripe webhook event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Session session) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeCustomerId(session.getCustomer());
        if (subscriptionOpt.isEmpty()) {
            log.warn("checkout.session.completed for unknown Stripe customer {}", session.getCustomer());
            return;
        }
        Subscription subscription = subscriptionOpt.get();
        subscription.setStripeSubscriptionId(session.getSubscription());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String planIdMeta = session.getMetadata() != null ? session.getMetadata().get("planId") : null;
        if (planIdMeta != null) {
            subscription.setPlanId(PlanId.valueOf(planIdMeta));
        }
        subscriptionRepository.save(subscription);
    }

    private void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId()).ifPresentOrElse(subscription -> {
            subscription.setStatus(mapStatus(stripeSubscription.getStatus()));
            if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
                if (periodEnd != null) {
                    subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
                }
            }
            subscriptionRepository.save(subscription);
        }, () -> log.warn("customer.subscription.updated for unknown Stripe subscription {}", stripeSubscription.getId()));
    }

    private void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId()).ifPresentOrElse(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);
        }, () -> log.warn("customer.subscription.deleted for unknown Stripe subscription {}", stripeSubscription.getId()));
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "active" -> SubscriptionStatus.ACTIVE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            // "past_due", "unpaid", "incomplete", "incomplete_expired" and any
            // future Stripe status all conservatively map to PAST_DUE rather
            // than ACTIVE - erring toward blocking access, not granting it.
            default -> SubscriptionStatus.PAST_DUE;
        };
    }

    private String priceIdFor(PlanId planId) {
        return switch (planId) {
            case STARTER -> stripeProperties.priceIdStarter();
            case PRO -> stripeProperties.priceIdPro();
            case FREE_TRIAL -> throw new IllegalArgumentException("Cannot check out into the free trial plan");
        };
    }

    private Subscription requireSubscription(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getOrganization() == null) {
            throw new UserNotInOrganizationException("You do not belong to an organization");
        }
        return subscriptionRepository.findByOrganizationId(user.getOrganization().getId())
                .orElseThrow(() -> new IllegalStateException("Organization " + user.getOrganization().getId() + " has no subscription row"));
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        boolean active = subscription.getStatus() == SubscriptionStatus.ACTIVE
                || (subscription.getStatus() == SubscriptionStatus.TRIALING
                        && subscription.getTrialEndsAt() != null
                        && subscription.getTrialEndsAt().isAfter(Instant.now()));
        return new SubscriptionResponse(
                subscription.getPlanId().name(),
                subscription.getStatus().name(),
                subscription.getTrialEndsAt(),
                subscription.getCurrentPeriodEnd(),
                active);
    }
}
