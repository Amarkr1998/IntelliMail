package com.intellimail.mail.controller;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.organization.CreateOrganizationRequest;
import com.intellimail.mail.dto.organization.OrganizationMemberResponse;
import com.intellimail.mail.dto.organization.OrganizationResponse;
import com.intellimail.mail.dto.organization.SlugAvailabilityResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.OrganizationService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization/workspace management - entirely opt-in, solo users are unaffected")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @Operation(summary = "Create an organization", description = "The caller becomes OWNER. Fails if the caller already belongs to an organization.")
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrganizationRequest request,
            HttpServletRequest httpRequest) {
        OrganizationResponse response = organizationService.createOrganization(principal.getId(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Organization created", response));
    }

    @GetMapping("/slug-available")
    @Operation(summary = "Check slug availability", description = "For live client-side validation before submitting the create-organization form.")
    public ApiResponse<SlugAvailabilityResponse> checkSlugAvailability(@RequestParam String slug) {
        return ApiResponse.success(organizationService.checkSlugAvailability(slug));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the caller's organization")
    public ApiResponse<OrganizationResponse> getMyOrganization(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(organizationService.getMyOrganization(principal.getId()));
    }

    @GetMapping("/members")
    @Operation(summary = "List the caller's organization members")
    public ApiResponse<PageResponse<OrganizationMemberResponse>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(organizationService.getMembers(principal.getId(), pageable));
    }

    @DeleteMapping("/members/{userId}")
    @PreAuthorize("@orgSecurity.isOwnerOrAdmin(authentication)")
    @Operation(summary = "Remove a member", description = "OWNER/ADMIN only. Cannot remove the organization's only remaining OWNER.")
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        organizationService.removeMember(principal.getId(), userId, httpRequest);
        return ApiResponse.success("Member removed", null);
    }
}
