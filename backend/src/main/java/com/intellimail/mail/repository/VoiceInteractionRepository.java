package com.intellimail.mail.repository;

import com.intellimail.mail.entity.VoiceInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoiceInteractionRepository extends JpaRepository<VoiceInteraction, UUID> {

    Page<VoiceInteraction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
