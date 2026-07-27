package com.intellimail.mail.entity;

import com.intellimail.mail.enums.OrgRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @jakarta.persistence.Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-hashed password. Never expose this field outside the entity/security layer. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Deliberate v1 scope decision: one organization per user, modeled as a
     * direct column rather than a membership join table. Multi-org
     * membership, if ever needed, would migrate this into a proper
     * {@code organization_memberships} table - not a concern this feature
     * takes on. Both fields are nullable together (see the DB CHECK
     * constraint in V14): organization membership is entirely opt-in, and a
     * user with {@code organization == null} behaves exactly as it did
     * before this feature existed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_role", length = 20)
    private OrgRole orgRole;

    /** Set once a user links or auto-registers via Google Sign-In; null otherwise. */
    @Column(name = "google_subject_id", unique = true, length = 255)
    private String googleSubjectId;
}
