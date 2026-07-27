package com.intellimail.mail.service;

import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.voice.VoicePromptRequest;
import com.intellimail.mail.dto.voice.VoiceResponse;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.entity.VoiceInteraction;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.logging.UsageAnalyticsRecorder;
import com.intellimail.mail.mapper.VoiceMapper;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.prompt.PromptFactory;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.repository.VoiceInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates the Voice AI feature: a speech-to-text transcript captured
 * client-side (Web Speech API) is turned into an AI response via Azure
 * OpenAI, and both are persisted together as one {@link VoiceInteraction}
 * row. Mirrors {@link EmailService}'s call/persist/record-analytics shape,
 * but a voice prompt is a single self-contained turn — there's no
 * regeneration or reply-attempt history to manage.
 */
@Service
@RequiredArgsConstructor
public class VoiceAiService {

    private final VoiceInteractionRepository voiceInteractionRepository;
    private final UserRepository userRepository;
    private final UsageAnalyticsRecorder usageAnalyticsRecorder;
    private final PromptFactory promptFactory;
    private final AzureOpenAiClient azureOpenAiClient;
    private final AiProperties aiProperties;
    private final VoiceMapper voiceMapper;

    @Transactional
    public VoiceResponse processVoicePrompt(UUID userId, VoicePromptRequest request) {
        User user = userRepository.getReferenceById(userId);
        PreparedPrompt prompt = promptFactory.forVoiceCommand(request.transcript(), request.language());

        try {
            AiGenerationResult result = azureOpenAiClient.generate(
                    prompt, aiProperties.defaultTemperature(), aiProperties.defaultMaxTokens());

            VoiceInteraction interaction = voiceInteractionRepository.save(VoiceInteraction.builder()
                    .user(user)
                    .transcript(request.transcript())
                    .aiResponse(result.content())
                    .language(request.language())
                    .aiModel(result.model())
                    .promptTokens(result.promptTokens())
                    .completionTokens(result.completionTokens())
                    .totalTokens(result.totalTokens())
                    .latencyMs(result.latencyMs())
                    .build());

            usageAnalyticsRecorder.recordSuccess(user, RequestType.VOICE_COMMAND, result.totalTokens(), result.latencyMs());

            return voiceMapper.toResponse(interaction);
        } catch (AiGenerationException ex) {
            usageAnalyticsRecorder.recordFailure(user, RequestType.VOICE_COMMAND, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<VoiceResponse> getHistory(UUID userId, Pageable pageable) {
        Page<VoiceInteraction> page = voiceInteractionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, voiceMapper::toResponse);
    }
}
