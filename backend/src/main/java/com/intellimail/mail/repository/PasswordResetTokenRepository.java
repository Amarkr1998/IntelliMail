package com.intellimail.mail.repository;

import com.intellimail.mail.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

    /**
     * Marks every still-unused token for a user as consumed — used both when
     * issuing a fresh token (closing the window where multiple valid reset
     * links could coexist) and after a successful reset (so an older,
     * still-unexpired email link from before the password changed can't be
     * used afterward).
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllUnusedForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
