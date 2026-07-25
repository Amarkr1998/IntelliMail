package com.intellimail.mail.service;

import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.exception.UnauthorizedActionException;
import com.intellimail.mail.mapper.PromptTemplateMapper;
import com.intellimail.mail.mapper.PromptTemplateMapperImpl;
import com.intellimail.mail.repository.PromptTemplateRepository;
import com.intellimail.mail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;
    @Mock
    private UserRepository userRepository;

    private final PromptTemplateMapper promptTemplateMapper = new PromptTemplateMapperImpl();

    private PromptTemplateService promptTemplateService;
    private User owner;

    @BeforeEach
    void setUp() {
        promptTemplateService = new PromptTemplateService(promptTemplateRepository, userRepository, promptTemplateMapper);
        owner = User.builder().fullName("Owner").email("owner@intellimail.com").password("hashed").build();
        owner.setId(UUID.randomUUID());
    }

    @Test
    void createTemplate_assignsCallerAsOwner() {
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenAnswer(invocation -> {
            PromptTemplate template = invocation.getArgument(0);
            template.setId(UUID.randomUUID());
            return template;
        });

        PromptTemplateRequest request = new PromptTemplateRequest(
                "Cold Outreach v1", "desc", RequestType.COLD_OUTREACH, "Write about {{topic}}", null, false);

        PromptTemplateResponse response = promptTemplateService.createTemplate(owner.getId(), request);

        assertThat(response.ownerId()).isEqualTo(owner.getId());
        assertThat(response.name()).isEqualTo("Cold Outreach v1");

        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(promptTemplateRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void updateTemplate_appliesChanges_whenCallerOwnsTemplate() {
        PromptTemplate existing = PromptTemplate.builder()
                .name("Old name")
                .category(RequestType.THANK_YOU)
                .promptText("Old prompt")
                .owner(owner)
                .isPublic(false)
                .build();
        existing.setId(UUID.randomUUID());

        when(promptTemplateRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(promptTemplateRepository.save(existing)).thenReturn(existing);

        PromptTemplateRequest request = new PromptTemplateRequest(
                "New name", "desc", RequestType.THANK_YOU, "New prompt", null, true);

        PromptTemplateResponse response = promptTemplateService.updateTemplate(owner.getId(), existing.getId(), request);

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.isPublic()).isTrue();
        assertThat(existing.getPromptText()).isEqualTo("New prompt");
    }

    @Test
    void updateTemplate_throwsUnauthorized_whenCallerDoesNotOwnTemplate() {
        User otherUser = User.builder().fullName("Other").email("other@intellimail.com").password("hashed").build();
        otherUser.setId(UUID.randomUUID());

        PromptTemplate existing = PromptTemplate.builder()
                .name("Name")
                .category(RequestType.SALES)
                .promptText("Prompt")
                .owner(otherUser)
                .isPublic(true)
                .build();
        existing.setId(UUID.randomUUID());

        when(promptTemplateRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        PromptTemplateRequest request = new PromptTemplateRequest(
                "Hijacked", "desc", RequestType.SALES, "Hijacked prompt", null, true);

        assertThatThrownBy(() -> promptTemplateService.updateTemplate(owner.getId(), existing.getId(), request))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(promptTemplateRepository, never()).save(any());
    }

    @Test
    void deleteTemplate_throwsNotFound_whenTemplateDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(promptTemplateRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promptTemplateService.deleteTemplate(owner.getId(), missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
