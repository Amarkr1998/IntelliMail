package com.intellimail.mail.entity;

import com.intellimail.mail.enums.OrgRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A single-use, hashed, expiring invitation to join an {@link Organization} -
 * the same token scheme as {@link PasswordResetToken} (SHA-256 hash
 * persisted, raw token only ever exists in the emailed link).
 */
@Entity
@Table(name = "organization_invitations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OrganizationInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_role", nullable = false, length = 20)
    private OrgRole orgRole;

    // SET NULL (not CASCADE): the inviter later leaving the organization
    // must not delete a still-pending invitation they issued.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User invitedBy;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;
}
