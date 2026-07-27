package com.intellimail.mail.service;

import com.intellimail.mail.config.AppProperties;
import com.intellimail.mail.dto.organization.AcceptInvitationRequest;
import com.intellimail.mail.dto.organization.InviteMemberRequest;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.OrganizationInvitation;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.exception.InvalidTokenException;
import com.intellimail.mail.exception.UserAlreadyInOrganizationException;
import com.intellimail.mail.exception.UserNotFoundException;
import com.intellimail.mail.exception.UserNotInOrganizationException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.OrganizationInvitationRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * A full peer to {@link OrganizationService} rather than folded into it, for
 * the same reason {@code PasswordResetService} is separate from
 * {@code AuthService}: this needs a {@link MailService} collaborator nothing
 * else in the organization feature does. Token scheme (raw token emailed,
 * only its SHA-256 hash persisted, single-use via a nullable timestamp) is
 * the exact same one {@code PasswordResetService} already established.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationInvitationService {

    private static final int TOKEN_BYTES = 32;
    private static final long INVITATION_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final MailService mailService;
    private final AppProperties appProperties;
    private final AuditLogRecorder auditLogRecorder;

    @Transactional
    public void invite(UUID actingUserId, InviteMemberRequest request, HttpServletRequest httpRequest) {
        User actingUser = userRepository.findById(actingUserId).orElseThrow(() -> new UserNotFoundException(actingUserId));
        Organization organization = actingUser.getOrganization();
        if (organization == null) {
            throw new UserNotInOrganizationException("You do not belong to an organization");
        }

        Instant now = Instant.now();
        invitationRepository.invalidatePendingForEmail(organization.getId(), request.email(), now);

        String rawToken = generateRawToken();
        invitationRepository.save(OrganizationInvitation.builder()
                .organization(organization)
                .email(request.email())
                .orgRole(request.orgRole())
                .invitedBy(actingUser)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS))
                .build());

        String inviteLink = appProperties.frontendUrl() + "/accept-invitation?token=" + rawToken;
        mailService.sendOrganizationInvitationEmail(request.email(), organization.getName(), inviteLink, organization.getBrandColor());

        auditLogRecorder.record(actingUser, "ORGANIZATION_INVITATION_SENT", "Organization", organization.getId().toString(), null, httpRequest);
    }

    @Transactional
    public OrganizationResponse accept(UUID userId, AcceptInvitationRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getOrganization() != null) {
            throw new UserAlreadyInOrganizationException("You already belong to an organization");
        }

        Instant now = Instant.now();
        OrganizationInvitation invitation = invitationRepository.findByTokenHash(hash(request.token()))
                .filter(i -> i.getAcceptedAt() == null && i.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation"));

        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvalidTokenException("This invitation was issued to a different email address");
        }

        invitation.setAcceptedAt(now);
        invitationRepository.save(invitation);

        Organization organization = invitation.getOrganization();
        user.setOrganization(organization);
        user.setOrgRole(invitation.getOrgRole());
        userRepository.save(user);

        auditLogRecorder.record(user, "ORGANIZATION_INVITATION_ACCEPTED", "Organization", organization.getId().toString(), null, httpRequest);

        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl(),
                organization.getBrandColor(),
                organization.getCreatedAt());
    }

    private String generateRawToken() {
        byte[] bytes = KeyGenerators.secureRandom(TOKEN_BYTES).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
