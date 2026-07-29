package com.intellimail.mail.repository;

import com.intellimail.mail.entity.EmailRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailRequestRepository extends JpaRepository<EmailRequest, UUID> {

    Page<EmailRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    /** Backs SearchHistoryAgentTool - a simple keyword search, not RAG/vector search. */
    Page<EmailRequest> findByUserIdAndOriginalContentContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID userId, String keyword, Pageable pageable);
}
