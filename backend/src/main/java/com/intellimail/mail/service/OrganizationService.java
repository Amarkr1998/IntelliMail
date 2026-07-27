package com.intellimail.mail.service;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.organization.CreateOrganizationRequest;
import com.intellimail.mail.dto.organization.OrganizationMemberResponse;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.dto.organization.SlugAvailabilityResponse;
import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.exception.CannotRemoveSoleOwnerException;
import com.intellimail.mail.exception.OrganizationSlugTakenException;
import com.intellimail.mail.exception.UserAlreadyInOrganizationException;
import com.intellimail.mail.exception.UserNotFoundException;
import com.intellimail.mail.exception.UserNotInOrganizationException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.repository.OrganizationRepository;
import com.intellimail.mail.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Organization membership is a deliberate v1 scope decision (see
 * {@link User}'s Javadoc): one organization per user, modeled as a direct
 * column rather than a membership table. Every method here treats the
 * currently-authenticated user's own {@code User.organization} as the source
 * of truth for "their" organization - there's no separate lookup-by-org-id
 * path in this feature, since a user can only ever act on their own org.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditLogRecorder auditLogRecorder;
    private final BillingService billingService;

    @Transactional
    public OrganizationResponse createOrganization(UUID userId, CreateOrganizationRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getOrganization() != null) {
            throw new UserAlreadyInOrganizationException("You already belong to an organization");
        }
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new OrganizationSlugTakenException(request.slug());
        }

        Organization organization = organizationRepository.save(Organization.builder()
                .name(request.name())
                .slug(request.slug())
                .build());

        user.setOrganization(organization);
        user.setOrgRole(OrgRole.OWNER);
        userRepository.save(user);

        billingService.createTrialSubscription(organization);

        auditLogRecorder.record(user, "ORGANIZATION_CREATED", "Organization", organization.getId().toString(), null, httpRequest);

        return toResponse(organization);
    }

    @Transactional(readOnly = true)
    public SlugAvailabilityResponse checkSlugAvailability(String slug) {
        return new SlugAvailabilityResponse(!organizationRepository.existsBySlug(slug));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getMyOrganization(UUID userId) {
        return toResponse(requireOrganization(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationMemberResponse> getMembers(UUID userId, Pageable pageable) {
        Organization organization = requireOrganization(userId);
        Page<User> members = userRepository.findByOrganizationId(organization.getId(), pageable);
        return PageResponse.from(members, this::toMemberResponse);
    }

    @Transactional
    public void removeMember(UUID actingUserId, UUID targetUserId, HttpServletRequest httpRequest) {
        User actingUser = userRepository.findById(actingUserId).orElseThrow(() -> new UserNotFoundException(actingUserId));
        Organization organization = requireOrganization(actingUser);

        User target = userRepository.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(targetUserId));
        if (target.getOrganization() == null || !target.getOrganization().getId().equals(organization.getId())) {
            throw new UserNotFoundException(targetUserId);
        }

        if (target.getOrgRole() == OrgRole.OWNER
                && userRepository.countByOrganizationIdAndOrgRole(organization.getId(), OrgRole.OWNER) <= 1) {
            throw new CannotRemoveSoleOwnerException("Cannot remove the only remaining owner of the organization");
        }

        target.setOrganization(null);
        target.setOrgRole(null);
        userRepository.save(target);

        auditLogRecorder.record(actingUser, "ORGANIZATION_MEMBER_REMOVED", "User", target.getId().toString(), null, httpRequest);
    }

    private Organization requireOrganization(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return requireOrganization(user);
    }

    private Organization requireOrganization(User user) {
        if (user.getOrganization() == null) {
            throw new UserNotInOrganizationException("You do not belong to an organization");
        }
        return user.getOrganization();
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl(),
                organization.getBrandColor(),
                organization.getCreatedAt());
    }

    private OrganizationMemberResponse toMemberResponse(User user) {
        return new OrganizationMemberResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getOrgRole() != null ? user.getOrgRole().name() : null);
    }
}
