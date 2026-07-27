package com.intellimail.mail.service;

import com.intellimail.mail.config.AppProperties;
import com.intellimail.mail.config.StripeProperties;
import com.intellimail.mail.dto.billing.CreateCheckoutSessionRequest;
import com.intellimail.mail.dto.billing.SubscriptionResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.Subscription;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.enums.PlanId;
import com.intellimail.mail.enums.SubscriptionStatus;
import com.intellimail.mail.exception.NoBillingAccountException;
import com.intellimail.mail.repository.SubscriptionRepository;
import com.intellimail.mail.repository.UserRepository;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;

    private final StripeProperties stripeProperties = new StripeProperties("sk_test", "whsec_test", "price_starter", "price_pro");
    private final AppProperties appProperties = new AppProperties("http://localhost:5173");

    private BillingService billingService;
    private Organization organization;
    private User user;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(subscriptionRepository, userRepository, stripeProperties, appProperties);

        organization = Organization.builder().name("Acme").slug("acme").build();
        organization.setId(UUID.randomUUID());

        user = User.builder().fullName("Owner").email("owner@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setOrgRole(OrgRole.OWNER);
    }

    private Subscription trialingSubscription(Instant trialEndsAt) {
        Subscription subscription = Subscription.builder()
                .organization(organization)
                .planId(PlanId.FREE_TRIAL)
                .status(SubscriptionStatus.TRIALING)
                .trialEndsAt(trialEndsAt)
                .build();
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    @Test
    void createTrialSubscription_savesTrialingSubscription() {
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription subscription = billingService.createTrialSubscription(organization);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(subscription.getPlanId()).isEqualTo(PlanId.FREE_TRIAL);
        assertThat(subscription.getTrialEndsAt()).isAfter(Instant.now());
        assertThat(subscription.getStripeCustomerId()).isNull();
    }

    @Test
    void getSubscription_active_whenTrialingAndNotExpired() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(trialingSubscription(Instant.now().plus(5, ChronoUnit.DAYS))));

        SubscriptionResponse response = billingService.getSubscription(user.getId());

        assertThat(response.active()).isTrue();
        assertThat(response.status()).isEqualTo("TRIALING");
    }

    @Test
    void getSubscription_inactive_whenTrialExpired() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(trialingSubscription(Instant.now().minus(1, ChronoUnit.HOURS))));

        SubscriptionResponse response = billingService.getSubscription(user.getId());

        assertThat(response.active()).isFalse();
    }

    @Test
    void createCheckoutSession_throwsIllegalArgument_forFreeTrialPlan() {
        assertThatThrownBy(() -> billingService.createCheckoutSession(user.getId(), new CreateCheckoutSessionRequest(PlanId.FREE_TRIAL)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCheckoutSession_createsStripeCustomer_whenNoneExistsYet() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Subscription subscription = trialingSubscription(Instant.now().plus(5, ChronoUnit.DAYS));
        when(subscriptionRepository.findByOrganizationId(organization.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer fakeCustomer = new Customer();
        fakeCustomer.setId("cus_new123");
        Session fakeSession = new Session();
        fakeSession.setUrl("https://checkout.stripe.com/fake-session");

        try (MockedStatic<Customer> customerStatic = Mockito.mockStatic(Customer.class);
             MockedStatic<Session> sessionStatic = Mockito.mockStatic(Session.class)) {
            customerStatic.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenReturn(fakeCustomer);
            sessionStatic.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class))).thenReturn(fakeSession);

            var response = billingService.createCheckoutSession(user.getId(), new CreateCheckoutSessionRequest(PlanId.STARTER));

            assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/fake-session");
            assertThat(subscription.getStripeCustomerId()).isEqualTo("cus_new123");
        }
    }

    @Test
    void createPortalSession_throwsNoBillingAccount_whenNoStripeCustomerYet() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(trialingSubscription(Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> billingService.createPortalSession(user.getId()))
                .isInstanceOf(NoBillingAccountException.class);
    }

    @Test
    void handleWebhookEvent_checkoutSessionCompleted_activatesSubscription() {
        Subscription subscription = trialingSubscription(Instant.now().plus(5, ChronoUnit.DAYS));
        subscription.setStripeCustomerId("cus_abc");
        when(subscriptionRepository.findByStripeCustomerId("cus_abc")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session session = new Session();
        session.setCustomer("cus_abc");
        session.setSubscription("sub_xyz");
        session.setMetadata(Map.of("planId", "STARTER"));

        Event event = Mockito.mock(Event.class);
        EventDataObjectDeserializer deserializer = Mockito.mock(EventDataObjectDeserializer.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        billingService.handleWebhookEvent(event);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(captor.getValue().getStripeSubscriptionId()).isEqualTo("sub_xyz");
        assertThat(captor.getValue().getPlanId()).isEqualTo(PlanId.STARTER);
    }

    @Test
    void handleWebhookEvent_subscriptionDeleted_marksCanceled() {
        Subscription subscription = trialingSubscription(Instant.now().plus(5, ChronoUnit.DAYS));
        subscription.setStripeSubscriptionId("sub_xyz");
        when(subscriptionRepository.findByStripeSubscriptionId("sub_xyz")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.stripe.model.Subscription stripeSubscription = new com.stripe.model.Subscription();
        stripeSubscription.setId("sub_xyz");

        Event event = Mockito.mock(Event.class);
        EventDataObjectDeserializer deserializer = Mockito.mock(EventDataObjectDeserializer.class);
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        billingService.handleWebhookEvent(event);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void handleWebhookEvent_unhandledEventType_doesNothing() {
        Event event = Mockito.mock(Event.class);
        when(event.getType()).thenReturn("customer.created");

        billingService.handleWebhookEvent(event);

        verify(subscriptionRepository, never()).save(any());
    }
}
