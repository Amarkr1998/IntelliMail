package com.intellimail.mail.service;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.exception.UnauthorizedActionException;
import com.intellimail.mail.mapper.PromptTemplateMapper;
import com.intellimail.mail.repository.PromptTemplateRepository;
import com.intellimail.mail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Templates authored by a user are private by default; {@code isPublic}
 * templates (any owner, including a future "system" seed user) are visible
 * to everyone via {@link PromptTemplateRepository#findVisibleToUser}.
 * Unlike Module 7's read-only template lookup during generation (which 404s
 * on a private template to avoid confirming its existence), mutating an
 * endpoint you don't own here returns 403 — you're acting on an id you
 * already retrieved from your own template list, so there's nothing left to
 * hide.
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final UserRepository userRepository;
    private final PromptTemplateMapper promptTemplateMapper;

    @Transactional(readOnly = true)
    public PageResponse<PromptTemplateResponse> getTemplates(UUID userId, UUID organizationId, Pageable pageable) {
        Page<PromptTemplate> page = promptTemplateRepository.findVisibleToUser(userId, organizationId, pageable);
        return PageResponse.from(page, promptTemplateMapper::toResponse);
    }

    @Transactional
    public PromptTemplateResponse createTemplate(UUID userId, UUID organizationId, PromptTemplateRequest request) {
        User owner = userRepository.getReferenceById(userId);
        PromptTemplate template = promptTemplateMapper.toEntity(request);
        template.setOwner(owner);
        template.setOrganizationId(organizationId);
        return promptTemplateMapper.toResponse(promptTemplateRepository.save(template));
    }

    @Transactional
    public PromptTemplateResponse updateTemplate(UUID userId, UUID templateId, PromptTemplateRequest request) {
        PromptTemplate template = findOwnedTemplate(userId, templateId);
        promptTemplateMapper.updateEntityFromRequest(request, template);
        return promptTemplateMapper.toResponse(promptTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID userId, UUID templateId) {
        findOwnedTemplate(userId, templateId);
        promptTemplateRepository.deleteById(templateId);
    }

    private PromptTemplate findOwnedTemplate(UUID userId, UUID templateId) {
        PromptTemplate template = promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", templateId));

        if (template.getOwner() == null || !template.getOwner().getId().equals(userId)) {
            throw new UnauthorizedActionException("You do not have permission to modify this template");
        }
        return template;
    }
}
