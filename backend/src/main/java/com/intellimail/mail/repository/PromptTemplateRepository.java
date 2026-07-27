package com.intellimail.mail.repository;

import com.intellimail.mail.entity.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    /**
     * Templates visible to a user: their own private templates, every
     * genuinely-global public template ({@code organization_id IS NULL} -
     * includes every template that existed before organizations did), plus -
     * for org members only - public templates scoped to their own
     * organization. {@code organizationId} is {@code null} for solo users,
     * which the {@code :organizationId IS NOT NULL} guard turns into exactly
     * today's pre-multi-tenancy behavior (never matches, so only the first
     * two clauses apply).
     */
    @Query("""
            SELECT pt FROM PromptTemplate pt
            WHERE pt.owner.id = :userId
               OR (pt.isPublic = true AND pt.organizationId IS NULL)
               OR (:organizationId IS NOT NULL AND pt.isPublic = true AND pt.organizationId = :organizationId)
            """)
    Page<PromptTemplate> findVisibleToUser(@Param("userId") UUID userId,
                                            @Param("organizationId") UUID organizationId,
                                            Pageable pageable);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
