package com.intellimail.mail.repository;

import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GeneratedReplyRepositoryTest {

    @Autowired
    private GeneratedReplyRepository generatedReplyRepository;

    @Autowired
    private EmailRequestRepository emailRequestRepository;

    @Autowired
    private UserRepository userRepository;

    private EmailRequest persistEmailRequest() {
        User user = userRepository.save(User.builder()
                .fullName("Test User")
                .email("test-" + System.nanoTime() + "@intellimail.com")
                .password("hashed-password")
                .build());

        return emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent("Can we reschedule our meeting?")
                .build());
    }

    @Test
    void findByEmailRequestIdOrderByAttemptNumberAsc_returnsRepliesInAttemptOrder() {
        EmailRequest request = persistEmailRequest();
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("v2").attemptNumber(2).build());
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("v1").attemptNumber(1).build());

        List<GeneratedReply> replies = generatedReplyRepository.findByEmailRequestIdOrderByAttemptNumberAsc(request.getId());

        assertThat(replies).extracting(GeneratedReply::getAttemptNumber).containsExactly(1, 2);
    }

    @Test
    void findMaxAttemptNumber_returnsHighestAttempt_forRegeneration() {
        EmailRequest request = persistEmailRequest();
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("v1").attemptNumber(1).build());
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("v2").attemptNumber(2).build());
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("v3").attemptNumber(3).build());

        Integer maxAttempt = generatedReplyRepository.findMaxAttemptNumber(request.getId());

        assertThat(maxAttempt).isEqualTo(3);
    }

    @Test
    void findFavoritesByUserId_returnsOnlyFavoritedReplies() {
        EmailRequest request = persistEmailRequest();
        generatedReplyRepository.save(GeneratedReply.builder().emailRequest(request).content("not favorite").favorite(false).build());
        GeneratedReply favorite = generatedReplyRepository.save(
                GeneratedReply.builder().emailRequest(request).content("favorite").favorite(true).build());

        List<GeneratedReply> favorites = generatedReplyRepository
                .findFavoritesByUserId(request.getUser().getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).getId()).isEqualTo(favorite.getId());
    }
}
