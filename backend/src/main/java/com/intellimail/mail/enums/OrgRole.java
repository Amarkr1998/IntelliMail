package com.intellimail.mail.enums;

/**
 * A user's role within their organization - orthogonal to the platform-wide
 * {@link RoleName} system (e.g. a platform ROLE_ADMIN has no special
 * org-level powers; these two dimensions are deliberately never conflated).
 */
public enum OrgRole {
    OWNER,
    ADMIN,
    MEMBER
}
