package com.intellimail.mail.repository;

import com.intellimail.mail.entity.AgentTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentTaskRepository extends JpaRepository<AgentTask, UUID> {

    Page<AgentTask> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<AgentTask> findByIdAndUserId(UUID id, UUID userId);
}
