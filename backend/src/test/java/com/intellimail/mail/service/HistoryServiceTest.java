package com.intellimail.mail.service;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.email.EmailHistoryResponse;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.mapper.EmailMapper;
import com.intellimail.mail.mapper.EmailMapperImpl;
import com.intellimail.mail.repository.EmailRequestRepository;
import com.intellimail.mail.repository.GeneratedReplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private EmailRequestRepository emailRequestRepository;
    @Mock
    private GeneratedReplyRepository generatedReplyRepository;

    private final EmailMapper emailMapper = new EmailMapperImpl();

    private HistoryService historyService;
    private User user;

    @BeforeEach
    void setUp() {
        historyService = new HistoryService(emailRequestRepository, generatedReplyRepository, emailMapper);
        user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void getHistory_assemblesEachRequestWithItsReplies() {
        EmailRequest emailRequest = EmailRequest.builder()
                .user(user)
                .requestType(RequestType.SUMMARIZE)
                .originalContent("Long email...")
                .build();
        emailRequest.setId(UUID.randomUUID());

        GeneratedReply reply = GeneratedReply.builder().emailRequest(emailRequest).content("Short summary").attemptNumber(1).build();
        reply.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 20);
        when(emailRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(emailRequest)));
        when(generatedReplyRepository.findByEmailRequestIdOrderByAttemptNumberAsc(emailRequest.getId()))
                .thenReturn(List.of(reply));

        PageResponse<EmailHistoryResponse> history = historyService.getHistory(user.getId(), pageable);

        assertThat(history.content()).hasSize(1);
        EmailHistoryResponse entry = history.content().get(0);
        assertThat(entry.requestType()).isEqualTo(RequestType.SUMMARIZE);
        assertThat(entry.replies()).hasSize(1);
        assertThat(entry.replies().get(0).content()).isEqualTo("Short summary");
    }

    @Test
    void deleteHistoryEntry_deletesWhenOwnedByCaller() {
        UUID requestId = UUID.randomUUID();
        when(emailRequestRepository.existsByIdAndUserId(requestId, user.getId())).thenReturn(true);

        historyService.deleteHistoryEntry(user.getId(), requestId);

        verify(emailRequestRepository).deleteById(requestId);
    }

    @Test
    void deleteHistoryEntry_throwsNotFound_whenNotOwnedByCaller() {
        UUID requestId = UUID.randomUUID();
        when(emailRequestRepository.existsByIdAndUserId(requestId, user.getId())).thenReturn(false);

        assertThatThrownBy(() -> historyService.deleteHistoryEntry(user.getId(), requestId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(emailRequestRepository, never()).deleteById(any());
    }

    @Test
    void setFavorite_togglesFlag_whenReplyBelongsToCaller() {
        EmailRequest emailRequest = EmailRequest.builder()
                .user(user)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent("Hi")
                .build();
        emailRequest.setId(UUID.randomUUID());

        GeneratedReply reply = GeneratedReply.builder().emailRequest(emailRequest).content("Reply").favorite(false).build();
        reply.setId(UUID.randomUUID());

        when(generatedReplyRepository.findById(reply.getId())).thenReturn(Optional.of(reply));
        when(generatedReplyRepository.save(reply)).thenReturn(reply);

        EmailReplyResponse response = historyService.setFavorite(user.getId(), reply.getId(), true);

        assertThat(response.favorite()).isTrue();
        assertThat(reply.isFavorite()).isTrue();
    }

    @Test
    void setFavorite_throwsNotFound_whenReplyBelongsToAnotherUser() {
        User otherUser = User.builder().fullName("Other").email("other@intellimail.com").password("hashed").build();
        otherUser.setId(UUID.randomUUID());

        EmailRequest otherRequest = EmailRequest.builder()
                .user(otherUser)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent("Hi")
                .build();
        otherRequest.setId(UUID.randomUUID());

        GeneratedReply reply = GeneratedReply.builder().emailRequest(otherRequest).content("Reply").build();
        reply.setId(UUID.randomUUID());

        when(generatedReplyRepository.findById(reply.getId())).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> historyService.setFavorite(user.getId(), reply.getId(), true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
