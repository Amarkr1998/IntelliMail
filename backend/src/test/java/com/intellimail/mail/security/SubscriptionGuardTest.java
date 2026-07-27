package com.intellimail.mail.security;

import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.Subscription;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.enums.PlanId;
import com.intellimail.mail.enums.SubscriptionStatus;
import com.intellimail.mail.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionGuardTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionGuard subscriptionGuard;
    private Organization organization;

    @BeforeEach
    void setUp() {
        subscriptionGuard = new SubscriptionGuard(subscriptionRepository);
        organization = Organization.builder().name("Acme").slug("acme").build();
        organization.setId(UUID.randomUUID());
    }

    private Authentication authenticationFor(Organization organization) {
        User user = User.builder().fullName("Test").email("test@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
        if (organization != null) {
            user.setOrganization(organization);
            user.setOrgRole(OrgRole.MEMBER);
        }
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(UserPrincipal.of(user));
        return authentication;
    }

    private Subscription subscriptionWith(SubscriptionStatus status, Instant trialEndsAt) {
        Subscription subscription = Subscription.builder()
                .organization(organization)
                .planId(PlanId.FREE_TRIAL)
                .status(status)
                .trialEndsAt(trialEndsAt)
                .build();
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    @Test
    void hasActiveAccess_trueForSoloUser_noOrganization() {
        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(null))).isTrue();
    }

    @Test
    void hasActiveAccess_trueWhenSubscriptionActive() {
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.ACTIVE, null)));

        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isTrue();
    }

    @Test
    void hasActiveAccess_trueWhenTrialingAndNotExpired() {
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.TRIALING, Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isTrue();
    }

    @Test
    void hasActiveAccess_falseWhenTrialExpired() {
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.TRIALING, Instant.now().minus(1, ChronoUnit.HOURS))));

        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isFalse();
    }

    @Test
    void hasActiveAccess_falseWhenPastDueOrCanceled() {
        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.PAST_DUE, null)));
        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isFalse();

        when(subscriptionRepository.findByOrganizationId(organization.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.CANCELED, null)));
        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isFalse();
    }

    @Test
    void hasActiveAccess_failsOpen_whenSubscriptionRowMissing() {
        when(subscriptionRepository.findByOrganizationId(any())).thenReturn(Optional.empty());

        assertThat(subscriptionGuard.hasActiveAccess(authenticationFor(organization))).isTrue();
    }
}
