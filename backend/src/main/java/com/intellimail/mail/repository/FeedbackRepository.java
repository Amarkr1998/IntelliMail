package com.intellimail.mail.repository;

import com.intellimail.mail.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByGeneratedReplyId(UUID generatedReplyId);

    List<Feedback> findByUserId(UUID userId);
}
