package com.intellimail.mail.security;

import com.intellimail.mail.entity.Organization;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgSecurityTest {

    private final OrgSecurity orgSecurity = new OrgSecurity();

    private Authentication authenticationFor(UUID organizationId, OrgRole orgRole) {
        User user = User.builder().fullName("Test").email("test@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
        if (organizationId != null) {
            Organization organization = Organization.builder().name("Org").slug("org").build();
            organization.setId(organizationId);
            user.setOrganization(organization);
        }
        user.setOrgRole(orgRole);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(UserPrincipal.of(user));
        return authentication;
    }

    @Test
    void isOwner_trueOnlyForOwner() {
        assertThat(orgSecurity.isOwner(authenticationFor(UUID.randomUUID(), OrgRole.OWNER))).isTrue();
        assertThat(orgSecurity.isOwner(authenticationFor(UUID.randomUUID(), OrgRole.ADMIN))).isFalse();
        assertThat(orgSecurity.isOwner(authenticationFor(UUID.randomUUID(), OrgRole.MEMBER))).isFalse();
    }

    @Test
    void isOwnerOrAdmin_trueForOwnerAndAdmin_falseForMember() {
        assertThat(orgSecurity.isOwnerOrAdmin(authenticationFor(UUID.randomUUID(), OrgRole.OWNER))).isTrue();
        assertThat(orgSecurity.isOwnerOrAdmin(authenticationFor(UUID.randomUUID(), OrgRole.ADMIN))).isTrue();
        assertThat(orgSecurity.isOwnerOrAdmin(authenticationFor(UUID.randomUUID(), OrgRole.MEMBER))).isFalse();
    }

    @Test
    void isOwnerOrAdmin_falseForSoloUser_noOrgRole() {
        assertThat(orgSecurity.isOwnerOrAdmin(authenticationFor(null, null))).isFalse();
    }

    @Test
    void isMemberOf_trueOnlyForMatchingOrganization() {
        UUID organizationId = UUID.randomUUID();
        Authentication authentication = authenticationFor(organizationId, OrgRole.MEMBER);

        assertThat(orgSecurity.isMemberOf(authentication, organizationId)).isTrue();
        assertThat(orgSecurity.isMemberOf(authentication, UUID.randomUUID())).isFalse();
    }

    @Test
    void isMemberOf_falseForSoloUser() {
        assertThat(orgSecurity.isMemberOf(authenticationFor(null, null), UUID.randomUUID())).isFalse();
    }
}
