package com.intellimail.mail.controller;

import com.intellimail.mail.dto.organization.AcceptInvitationRequest;
import com.intellimail.mail.dto.organization.InviteMemberRequest;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.OrganizationInvitationService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/invitations")
@RequiredArgsConstructor
@Tag(name = "Organization Invitations", description = "Invite members to an organization and accept invitations")
public class OrganizationInvitationController {

    private final OrganizationInvitationService organizationInvitationService;

    @PostMapping
    @PreAuthorize("@orgSecurity.isOwnerOrAdmin(authentication)")
    @Operation(summary = "Invite a member", description = "OWNER/ADMIN only. Sends an emailed invitation link.")
    public ResponseEntity<ApiResponse<Void>> invite(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InviteMemberRequest request,
            HttpServletRequest httpRequest) {
        organizationInvitationService.invite(principal.getId(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invitation sent", null));
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invitation", description = "Any authenticated user may accept - the invitation is only valid for the email address it was issued to.")
    public ApiResponse<OrganizationResponse> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AcceptInvitationRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success("Invitation accepted", organizationInvitationService.accept(principal.getId(), request, httpRequest));
    }
}
