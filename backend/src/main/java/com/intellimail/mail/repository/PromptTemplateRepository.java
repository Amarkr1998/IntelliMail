package com.intellimail.mail.repository;

import com.intellimail.mail.entity.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    /** Templates visible to a user: their own private templates plus every public/system template. */
    @Query("""
            SELECT pt FROM PromptTemplate pt
            WHERE pt.isPublic = true OR pt.owner.id = :userId
            """)
    Page<PromptTemplate> findVisibleToUser(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
