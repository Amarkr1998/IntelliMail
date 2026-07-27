package com.intellimail.mail.security;

import com.intellimail.mail.enums.OrgRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SpEL-usable org-membership/role checks for {@code @PreAuthorize}, e.g.
 * {@code @PreAuthorize("@orgSecurity.isOwnerOrAdmin(authentication)")}. Pure
 * in-memory checks against the already-fresh-per-request
 * {@link UserPrincipal} - no DB call here (see {@link UserPrincipal}'s
 * Javadoc for why that's always safe/never stale). The first real use of the
 * {@code @EnableMethodSecurity} infrastructure this app has had wired but
 * unused since it was first added.
 */
@Component("orgSecurity")
public class OrgSecurity {

    public boolean isMemberOf(Authentication authentication, UUID organizationId) {
        UserPrincipal principal = principal(authentication);
        return organizationId != null && organizationId.equals(principal.getOrganizationId());
    }

    public boolean isOwner(Authentication authentication) {
        return principal(authentication).getOrgRole() == OrgRole.OWNER;
    }

    public boolean isOwnerOrAdmin(Authentication authentication) {
        OrgRole role = principal(authentication).getOrgRole();
        return role == OrgRole.OWNER || role == OrgRole.ADMIN;
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
