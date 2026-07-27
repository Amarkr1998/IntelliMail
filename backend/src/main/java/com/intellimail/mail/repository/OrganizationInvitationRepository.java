package com.intellimail.mail.repository;

import com.intellimail.mail.entity.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

    Optional<OrganizationInvitation> findByTokenHash(String tokenHash);

    /** Closes the window where multiple valid invitations could coexist for the same org+email. */
    @Modifying
    @Query("""
            UPDATE OrganizationInvitation i SET i.acceptedAt = :now
            WHERE i.organization.id = :organizationId AND i.email = :email AND i.acceptedAt IS NULL
            """)
    void invalidatePendingForEmail(@Param("organizationId") UUID organizationId,
                                    @Param("email") String email,
                                    @Param("now") Instant now);
}
