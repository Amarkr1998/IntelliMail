package com.intellimail.mail.security;

import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/** Spring Security's view of an authenticated {@link User}, carrying just what auth/authorization needs. */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Read fresh from the {@link User} on every request (this class is
     * rebuilt per-request by {@code CustomUserDetailsService}, never cached
     * or sourced from JWT claims) - so these are never stale, unlike a claim
     * embedded in a 15-minute-lived access token would be.
     */
    private final UUID organizationId;
    private final OrgRole orgRole;

    private UserPrincipal(UUID id, String email, String password, boolean enabled,
                           Collection<? extends GrantedAuthority> authorities,
                           UUID organizationId, OrgRole orgRole) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.organizationId = organizationId;
        this.orgRole = orgRole;
    }

    public static UserPrincipal of(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());
        UUID organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.isEnabled(), authorities,
                organizationId, user.getOrgRole());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
