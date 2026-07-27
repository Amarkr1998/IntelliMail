package com.intellimail.mail.service;

import com.intellimail.mail.dto.organization.CreateOrganizationRequest;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.dto.organization.SlugAvailabilityResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.exception.CannotRemoveSoleOwnerException;
import com.intellimail.mail.exception.OrganizationSlugTakenException;
import com.intellimail.mail.exception.UserAlreadyInOrganizationException;
import com.intellimail.mail.exception.UserNotInOrganizationException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.OrganizationRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRecorder auditLogRecorder;
    @Mock
    private BillingService billingService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private OrganizationService organizationService;
    private User user;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository, userRepository, auditLogRecorder, billingService);
        user = User.builder().fullName("Ada Lovelace").email("ada@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void createOrganization_setsCallerAsOwner_andPersistsOrganization() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(organizationRepository.existsBySlug("acme")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization organization = invocation.getArgument(0);
            organization.setId(UUID.randomUUID());
            return organization;
        });

        OrganizationResponse response = organizationService.createOrganization(
                user.getId(), new CreateOrganizationRequest("Acme Inc", "acme"), httpServletRequest);

        assertThat(response.name()).isEqualTo("Acme Inc");
        assertThat(response.slug()).isEqualTo("acme");
        assertThat(user.getOrgRole()).isEqualTo(OrgRole.OWNER);
        assertThat(user.getOrganization()).isNotNull();
        verify(userRepository).save(user);
        verify(billingService).createTrialSubscription(any(Organization.class));
        verify(auditLogRecorder).record(eq(user), eq("ORGANIZATION_CREATED"), eq("Organization"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void createOrganization_throwsUserAlreadyInOrganization_whenCallerAlreadyHasOne() {
        Organization existing = Organization.builder().name("Existing").slug("existing").build();
        existing.setId(UUID.randomUUID());
        user.setOrganization(existing);
        user.setOrgRole(OrgRole.MEMBER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> organizationService.createOrganization(
                user.getId(), new CreateOrganizationRequest("New Org", "new-org"), httpServletRequest))
                .isInstanceOf(UserAlreadyInOrganizationException.class);

        verify(organizationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createOrganization_throwsSlugTaken_whenSlugAlreadyExists() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(organizationRepository.existsBySlug("acme")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(
                user.getId(), new CreateOrganizationRequest("Acme Inc", "acme"), httpServletRequest))
                .isInstanceOf(OrganizationSlugTakenException.class);
    }

    @Test
    void checkSlugAvailability_reflectsRepositoryState() {
        when(organizationRepository.existsBySlug("taken")).thenReturn(true);
        when(organizationRepository.existsBySlug("free")).thenReturn(false);

        assertThat(organizationService.checkSlugAvailability("taken")).isEqualTo(new SlugAvailabilityResponse(false));
        assertThat(organizationService.checkSlugAvailability("free")).isEqualTo(new SlugAvailabilityResponse(true));
    }

    @Test
    void getMyOrganization_throwsUserNotInOrganization_forSoloUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> organizationService.getMyOrganization(user.getId()))
                .isInstanceOf(UserNotInOrganizationException.class);
    }

    @Test
    void removeMember_throwsCannotRemoveSoleOwner_whenTargetIsOnlyOwner() {
        Organization organization = Organization.builder().name("Acme").slug("acme").build();
        organization.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setOrgRole(OrgRole.OWNER);

        User target = User.builder().fullName("Target").email("target@intellimail.com").password("hashed").build();
        target.setId(UUID.randomUUID());
        target.setOrganization(organization);
        target.setOrgRole(OrgRole.OWNER);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.countByOrganizationIdAndOrgRole(organization.getId(), OrgRole.OWNER)).thenReturn(1L);

        assertThatThrownBy(() -> organizationService.removeMember(user.getId(), target.getId(), httpServletRequest))
                .isInstanceOf(CannotRemoveSoleOwnerException.class);

        verify(userRepository, org.mockito.Mockito.never()).save(target);
    }

    @Test
    void removeMember_clearsOrganizationAndRole_whenNotSoleOwner() {
        Organization organization = Organization.builder().name("Acme").slug("acme").build();
        organization.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setOrgRole(OrgRole.OWNER);

        User target = User.builder().fullName("Target").email("target@intellimail.com").password("hashed").build();
        target.setId(UUID.randomUUID());
        target.setOrganization(organization);
        target.setOrgRole(OrgRole.MEMBER);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        organizationService.removeMember(user.getId(), target.getId(), httpServletRequest);

        assertThat(target.getOrganization()).isNull();
        assertThat(target.getOrgRole()).isNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(target);
    }
}
