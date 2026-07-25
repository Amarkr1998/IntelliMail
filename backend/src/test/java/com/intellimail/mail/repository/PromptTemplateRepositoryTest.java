package com.intellimail.mail.repository;

import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PromptTemplateRepositoryTest {

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder()
                .fullName("Test User")
                .email(email)
                .password("hashed")
                .build());
    }

    @Test
    void findVisibleToUser_includesOwnPrivateTemplates_andEveryPublicTemplate_butNotOthersPrivateTemplates() {
        User owner = persistUser("owner-" + UUID.randomUUID() + "@intellimail.com");
        User otherUser = persistUser("other-" + UUID.randomUUID() + "@intellimail.com");

        PromptTemplate ownPrivate = promptTemplateRepository.save(PromptTemplate.builder()
                .name("My private template")
                .category(RequestType.SALES)
                .promptText("...")
                .owner(owner)
                .isPublic(false)
                .build());

        PromptTemplate someonesPublic = promptTemplateRepository.save(PromptTemplate.builder()
                .name("Public system template")
                .category(RequestType.THANK_YOU)
                .promptText("...")
                .owner(otherUser)
                .isPublic(true)
                .build());

        promptTemplateRepository.save(PromptTemplate.builder()
                .name("Someone else's private template")
                .category(RequestType.HR)
                .promptText("...")
                .owner(otherUser)
                .isPublic(false)
                .build());

        List<PromptTemplate> visible = promptTemplateRepository
                .findVisibleToUser(owner.getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(visible).extracting(PromptTemplate::getId)
                .containsExactlyInAnyOrder(ownPrivate.getId(), someonesPublic.getId());
    }

    @Test
    void existsByIdAndOwnerId_reflectsActualOwnership() {
        User owner = persistUser("owner2-" + UUID.randomUUID() + "@intellimail.com");
        User otherUser = persistUser("other2-" + UUID.randomUUID() + "@intellimail.com");

        PromptTemplate template = promptTemplateRepository.save(PromptTemplate.builder()
                .name("Template")
                .category(RequestType.MARKETING)
                .promptText("...")
                .owner(owner)
                .isPublic(false)
                .build());

        assertThat(promptTemplateRepository.existsByIdAndOwnerId(template.getId(), owner.getId())).isTrue();
        assertThat(promptTemplateRepository.existsByIdAndOwnerId(template.getId(), otherUser.getId())).isFalse();
    }
}
