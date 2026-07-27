package com.intellimail.mail.service;

import com.intellimail.mail.config.AppProperties;
import com.intellimail.mail.dto.organization.AcceptInvitationRequest;
import com.intellimail.mail.dto.organization.InviteMemberRequest;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.OrganizationInvitation;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.exception.InvalidTokenException;
import com.intellimail.mail.exception.UserAlreadyInOrganizationException;
import com.intellimail.mail.exception.UserNotInOrganizationException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.OrganizationInvitationRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationInvitationRepository invitationRepository;
    @Mock
    private MailService mailService;
    @Mock
    private AuditLogRecorder auditLogRecorder;
    @Mock
    private HttpServletRequest httpServletRequest;

    private final AppProperties appProperties = new AppProperties("http://localhost:5173");

    private OrganizationInvitationService invitationService;
    private User actingUser;
    private Organization organization;

    @BeforeEach
    void setUp() {
        invitationService = new OrganizationInvitationService(
                userRepository, invitationRepository, mailService, appProperties, auditLogRecorder);

        organization = Organization.builder().name("Acme Inc").slug("acme").build();
        organization.setId(UUID.randomUUID());

        actingUser = User.builder().fullName("Owner").email("owner@intellimail.com").password("hashed").build();
        actingUser.setId(UUID.randomUUID());
        actingUser.setOrganization(organization);
        actingUser.setOrgRole(OrgRole.OWNER);
    }

    @Test
    void invite_savesInvitation_andSendsEmail() {
        when(userRepository.findById(actingUser.getId())).thenReturn(Optional.of(actingUser));

        invitationService.invite(actingUser.getId(),
                new InviteMemberRequest("newbie@intellimail.com", OrgRole.MEMBER), httpServletRequest);

        verify(invitationRepository).invalidatePendingForEmail(eq(organization.getId()), eq("newbie@intellimail.com"), any(Instant.class));

        ArgumentCaptor<OrganizationInvitation> captor = ArgumentCaptor.forClass(OrganizationInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("newbie@intellimail.com");
        assertThat(captor.getValue().getOrgRole()).isEqualTo(OrgRole.MEMBER);
        assertThat(captor.getValue().getInvitedBy()).isEqualTo(actingUser);
        assertThat(captor.getValue().getTokenHash()).hasSize(64);

        verify(mailService).sendOrganizationInvitationEmail(eq("newbie@intellimail.com"), eq("Acme Inc"), anyString(), any());
        verify(auditLogRecorder).record(eq(actingUser), eq("ORGANIZATION_INVITATION_SENT"), eq("Organization"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void invite_throwsUserNotInOrganization_whenActingUserIsSolo() {
        User soloUser = User.builder().fullName("Solo").email("solo@intellimail.com").password("hashed").build();
        soloUser.setId(UUID.randomUUID());
        when(userRepository.findById(soloUser.getId())).thenReturn(Optional.of(soloUser));

        assertThatThrownBy(() -> invitationService.invite(soloUser.getId(),
                new InviteMemberRequest("x@intellimail.com", OrgRole.MEMBER), httpServletRequest))
                .isInstanceOf(UserNotInOrganizationException.class);
    }

    @Test
    void accept_happyPath_joinsOrganization() {
        User invitee = User.builder().fullName("Invitee").email("invitee@intellimail.com").password("hashed").build();
        invitee.setId(UUID.randomUUID());

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(organization)
                .email("invitee@intellimail.com")
                .orgRole(OrgRole.MEMBER)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(invitation));

        OrganizationResponse response = invitationService.accept(invitee.getId(), new AcceptInvitationRequest("raw-token"), httpServletRequest);

        assertThat(response.slug()).isEqualTo("acme");
        assertThat(invitee.getOrganization()).isEqualTo(organization);
        assertThat(invitee.getOrgRole()).isEqualTo(OrgRole.MEMBER);
        assertThat(invitation.getAcceptedAt()).isNotNull();
        verify(auditLogRecorder).record(eq(invitee), eq("ORGANIZATION_INVITATION_ACCEPTED"), eq("Organization"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void accept_throwsInvalidToken_whenExpired() {
        User invitee = User.builder().fullName("Invitee").email("invitee@intellimail.com").password("hashed").build();
        invitee.setId(UUID.randomUUID());

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(organization)
                .email("invitee@intellimail.com")
                .orgRole(OrgRole.MEMBER)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.accept(invitee.getId(), new AcceptInvitationRequest("raw-token"), httpServletRequest))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void accept_throwsInvalidToken_whenEmailDoesNotMatchInvitee() {
        User invitee = User.builder().fullName("Invitee").email("someone-else@intellimail.com").password("hashed").build();
        invitee.setId(UUID.randomUUID());

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(organization)
                .email("invitee@intellimail.com")
                .orgRole(OrgRole.MEMBER)
                .tokenHash(sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.accept(invitee.getId(), new AcceptInvitationRequest("raw-token"), httpServletRequest))
                .isInstanceOf(InvalidTokenException.class);

        assertThat(invitee.getOrganization()).isNull();
    }

    @Test
    void accept_throwsUserAlreadyInOrganization_whenInviteeAlreadyHasOrg() {
        Organization otherOrg = Organization.builder().name("Other").slug("other").build();
        otherOrg.setId(UUID.randomUUID());

        User invitee = User.builder().fullName("Invitee").email("invitee@intellimail.com").password("hashed").build();
        invitee.setId(UUID.randomUUID());
        invitee.setOrganization(otherOrg);
        invitee.setOrgRole(OrgRole.MEMBER);

        when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));

        assertThatThrownBy(() -> invitationService.accept(invitee.getId(), new AcceptInvitationRequest("raw-token"), httpServletRequest))
                .isInstanceOf(UserAlreadyInOrganizationException.class);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
