package com.intellimail.mail.repository;

import com.intellimail.mail.entity.GeneratedReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GeneratedReplyRepository extends JpaRepository<GeneratedReply, UUID> {

    List<GeneratedReply> findByEmailRequestIdOrderByAttemptNumberAsc(UUID emailRequestId);

    @Query("""
            SELECT gr FROM GeneratedReply gr
            WHERE gr.emailRequest.user.id = :userId AND gr.favorite = true
            ORDER BY gr.createdAt DESC
            """)
    Page<GeneratedReply> findFavoritesByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT MAX(gr.attemptNumber) FROM GeneratedReply gr
            WHERE gr.emailRequest.id = :emailRequestId
            """)
    Integer findMaxAttemptNumber(@Param("emailRequestId") UUID emailRequestId);

    boolean existsByIdAndEmailRequestUserId(UUID id, UUID userId);
}
