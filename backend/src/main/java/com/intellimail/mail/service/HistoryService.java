package com.intellimail.mail.service;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.email.EmailHistoryResponse;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.mapper.EmailMapper;
import com.intellimail.mail.repository.EmailRequestRepository;
import com.intellimail.mail.repository.GeneratedReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read/delete access to a user's AI-generation history, plus Favorite
 * Replies. Reply Regeneration itself lives on {@link EmailService} — it
 * needs the same prompt-building machinery as the original /api/email/*
 * endpoints, so duplicating it here would just split one concern in two.
 */
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final EmailRequestRepository emailRequestRepository;
    private final GeneratedReplyRepository generatedReplyRepository;
    private final EmailMapper emailMapper;

    @Transactional(readOnly = true)
    public PageResponse<EmailHistoryResponse> getHistory(UUID userId, Pageable pageable) {
        Page<EmailRequest> page = emailRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, emailRequest -> emailMapper.toHistoryResponse(
                emailRequest, generatedReplyRepository.findByEmailRequestIdOrderByAttemptNumberAsc(emailRequest.getId())));
    }

    @Transactional
    public void deleteHistoryEntry(UUID userId, UUID emailRequestId) {
        if (!emailRequestRepository.existsByIdAndUserId(emailRequestId, userId)) {
            throw new ResourceNotFoundException("EmailRequest", emailRequestId);
        }
        emailRequestRepository.deleteById(emailRequestId);
    }

    @Transactional
    public EmailReplyResponse setFavorite(UUID userId, UUID replyId, boolean favorite) {
        GeneratedReply reply = generatedReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("GeneratedReply", replyId));
        if (!reply.getEmailRequest().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("GeneratedReply", replyId);
        }

        reply.setFavorite(favorite);
        return emailMapper.toReplyResponse(generatedReplyRepository.save(reply));
    }
}
