package com.intellimail.mail.service;

import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.email.EmailCustomRequest;
import com.intellimail.mail.dto.email.EmailFollowupRequest;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.email.EmailImproveRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSubjectRequest;
import com.intellimail.mail.dto.email.EmailSummarizeRequest;
import com.intellimail.mail.dto.email.EmailTranslateRequest;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.logging.UsageAnalyticsRecorder;
import com.intellimail.mail.mapper.EmailMapper;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.prompt.PromptFactory;
import com.intellimail.mail.repository.EmailRequestRepository;
import com.intellimail.mail.repository.GeneratedReplyRepository;
import com.intellimail.mail.repository.PromptTemplateRepository;
import com.intellimail.mail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates every /api/email/* endpoint: persist the request, build the
 * right prompt, call Azure OpenAI, persist the reply, and record usage
 * analytics — success or failure. Each public method maps 1:1 to one
 * controller endpoint and one {@link com.intellimail.mail.enums.RequestType}.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRequestRepository emailRequestRepository;
    private final GeneratedReplyRepository generatedReplyRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final UserRepository userRepository;
    private final UsageAnalyticsRecorder usageAnalyticsRecorder;
    private final PromptFactory promptFactory;
    private final AzureOpenAiClient azureOpenAiClient;
    private final AiProperties aiProperties;
    private final EmailMapper emailMapper;

    @Transactional
    public EmailReplyResponse generateReply(UUID userId, EmailGenerateRequest request) {
        User user = getUserRef(userId);
        PromptTemplate template = resolveTemplate(request.promptTemplateId(), userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent(request.originalContent())
                .instructions(request.instructions())
                .promptTemplate(template)
                .build());

        PreparedPrompt prompt = promptFactory.forGenerateReply(
                request.originalContent(), request.instructions(), templateSystemPrompt(template));

        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse improve(UUID userId, EmailImproveRequest request) {
        User user = getUserRef(userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(request.style())
                .originalContent(request.content())
                .build());

        PreparedPrompt prompt = promptFactory.forRewrite(request.style(), request.content(), null);
        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse translate(UUID userId, EmailTranslateRequest request) {
        User user = getUserRef(userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.TRANSLATE)
                .originalContent(request.content())
                .targetLanguage(request.targetLanguage())
                .build());

        PreparedPrompt prompt = promptFactory.forTranslate(request.content(), request.targetLanguage());
        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse summarize(UUID userId, EmailSummarizeRequest request) {
        User user = getUserRef(userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.SUMMARIZE)
                .originalContent(request.content())
                .build());

        PreparedPrompt prompt = promptFactory.forSummarize(request.content());
        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse subjectLine(UUID userId, EmailSubjectRequest request) {
        User user = getUserRef(userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.SUBJECT_LINE)
                .originalContent(request.content())
                .build());

        PreparedPrompt prompt = promptFactory.forSubjectLine(request.content());
        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse followup(UUID userId, EmailFollowupRequest request) {
        User user = getUserRef(userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(RequestType.FOLLOWUP)
                .originalContent(request.originalContent())
                .instructions(request.instructions())
                .build());

        PreparedPrompt prompt = promptFactory.forFollowup(request.originalContent(), request.instructions());
        return executeAndPersist(emailRequest, prompt, 1);
    }

    @Transactional
    public EmailReplyResponse custom(UUID userId, EmailCustomRequest request) {
        User user = getUserRef(userId);
        PromptTemplate template = resolveTemplate(request.promptTemplateId(), userId);

        EmailRequest emailRequest = emailRequestRepository.save(EmailRequest.builder()
                .user(user)
                .requestType(request.requestType())
                .originalContent(request.context())
                .instructions(request.customPrompt())
                .promptTemplate(template)
                .build());

        PreparedPrompt prompt = promptFactory.forCustomGenerator(
                request.requestType(), request.context(), request.customPrompt(), templateSystemPrompt(template));

        return executeAndPersist(emailRequest, prompt, 1);
    }

    /**
     * Regenerates a reply for an already-persisted {@code EmailRequest}
     * (Reply Regeneration): re-derives the same prompt the original request
     * would have built, and saves the result as a new {@code GeneratedReply}
     * row with the next attempt number, so every prior attempt is preserved.
     */
    @Transactional
    public EmailReplyResponse regenerate(UUID userId, UUID emailRequestId) {
        EmailRequest emailRequest = emailRequestRepository.findById(emailRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("EmailRequest", emailRequestId));
        if (!emailRequest.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("EmailRequest", emailRequestId);
        }

        Integer maxAttempt = generatedReplyRepository.findMaxAttemptNumber(emailRequestId);
        int nextAttempt = (maxAttempt == null ? 0 : maxAttempt) + 1;

        PreparedPrompt prompt = buildPromptForExistingRequest(emailRequest);
        return executeAndPersist(emailRequest, prompt, nextAttempt);
    }

    private PreparedPrompt buildPromptForExistingRequest(EmailRequest emailRequest) {
        RequestType type = emailRequest.getRequestType();
        String content = emailRequest.getOriginalContent();
        String instructions = emailRequest.getInstructions();
        String overrideSystemPrompt = templateSystemPrompt(emailRequest.getPromptTemplate());

        return switch (type) {
            case GENERATE_REPLY -> promptFactory.forGenerateReply(content, instructions, overrideSystemPrompt);
            case PROFESSIONAL_REWRITE, FRIENDLY_REWRITE, FORMAL_REWRITE, CASUAL_REWRITE, GRAMMAR_CORRECTION, EXPAND, SHORTEN ->
                    promptFactory.forRewrite(type, content, overrideSystemPrompt);
            case SUMMARIZE -> promptFactory.forSummarize(content);
            case TRANSLATE -> promptFactory.forTranslate(content, emailRequest.getTargetLanguage());
            case SUBJECT_LINE -> promptFactory.forSubjectLine(content);
            case FOLLOWUP -> promptFactory.forFollowup(content, instructions);
            case MEETING_REQUEST, THANK_YOU, APOLOGY, SALES, HR, MARKETING, COLD_OUTREACH, CUSTOM_PROMPT ->
                    promptFactory.forCustomGenerator(type, content, instructions, overrideSystemPrompt);
        };
    }

    private EmailReplyResponse executeAndPersist(EmailRequest emailRequest, PreparedPrompt prompt, int attemptNumber) {
        try {
            AiGenerationResult result = azureOpenAiClient.generate(
                    prompt, aiProperties.defaultTemperature(), aiProperties.defaultMaxTokens());

            GeneratedReply reply = generatedReplyRepository.save(GeneratedReply.builder()
                    .emailRequest(emailRequest)
                    .content(result.content())
                    .aiModel(result.model())
                    .attemptNumber(attemptNumber)
                    .promptTokens(result.promptTokens())
                    .completionTokens(result.completionTokens())
                    .totalTokens(result.totalTokens())
                    .latencyMs(result.latencyMs())
                    .build());

            usageAnalyticsRecorder.recordSuccess(
                    emailRequest.getUser(), emailRequest.getRequestType(), result.totalTokens(), result.latencyMs());

            return emailMapper.toReplyResponse(reply);
        } catch (AiGenerationException ex) {
            usageAnalyticsRecorder.recordFailure(emailRequest.getUser(), emailRequest.getRequestType(), ex.getMessage());
            throw ex;
        }
    }

    /** Returns null (no override) unless a visible template with a non-blank system prompt was supplied. */
    private String templateSystemPrompt(PromptTemplate template) {
        return template != null ? template.getSystemPrompt() : null;
    }

    /** A template is usable if it's a public/system template or the caller owns it; anything else 404s rather than 403s to avoid confirming a private template's existence. */
    private PromptTemplate resolveTemplate(UUID templateId, UUID userId) {
        if (templateId == null) {
            return null;
        }
        PromptTemplate template = promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", templateId));

        boolean visible = template.isPublic()
                || (template.getOwner() != null && template.getOwner().getId().equals(userId));
        if (!visible) {
            throw new ResourceNotFoundException("PromptTemplate", templateId);
        }
        return template;
    }

    private User getUserRef(UUID userId) {
        return userRepository.getReferenceById(userId);
    }
}
