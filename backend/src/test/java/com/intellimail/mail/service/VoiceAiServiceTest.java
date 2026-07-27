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
import com.intellimail.mail.mapper.VoiceMapperImpl;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.prompt.PromptFactory;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.repository.VoiceInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceAiServiceTest {

    @Mock
    private VoiceInteractionRepository voiceInteractionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UsageAnalyticsRecorder usageAnalyticsRecorder;
    @Mock
    private PromptFactory promptFactory;
    @Mock
    private AzureOpenAiClient azureOpenAiClient;

    private final VoiceMapper voiceMapper = new VoiceMapperImpl();
    private final AiProperties aiProperties = new AiProperties(0.7, 1024, true, new AiProperties.Retry(3, 500));

    private VoiceAiService voiceAiService;
    private User user;

    @BeforeEach
    void setUp() {
        voiceAiService = new VoiceAiService(
                voiceInteractionRepository, userRepository, usageAnalyticsRecorder, promptFactory,
                azureOpenAiClient, aiProperties, voiceMapper);

        user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void processVoicePrompt_happyPath_savesInteractionAndRecordsSuccess() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        stubSaveWithGeneratedId();

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forVoiceCommand("Draft a reply saying yes to the meeting", "English (US)")).thenReturn(prompt);

        AiGenerationResult result = new AiGenerationResult("Sure, that time works for me.", "gpt-4o", 40, 20, 60, 650L);
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenReturn(result);

        VoicePromptRequest request = new VoicePromptRequest("Draft a reply saying yes to the meeting", "English (US)");

        VoiceResponse response = voiceAiService.processVoicePrompt(user.getId(), request);

        assertThat(response.aiResponse()).isEqualTo("Sure, that time works for me.");
        assertThat(response.transcript()).isEqualTo("Draft a reply saying yes to the meeting");
        assertThat(response.language()).isEqualTo("English (US)");
        assertThat(response.totalTokens()).isEqualTo(60);
        verify(usageAnalyticsRecorder).recordSuccess(user, RequestType.VOICE_COMMAND, 60, 650L);
    }

    @Test
    void processVoicePrompt_withoutLanguage_passesNullThrough() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        stubSaveWithGeneratedId();

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forVoiceCommand(anyString(), eq(null))).thenReturn(prompt);

        AiGenerationResult result = new AiGenerationResult("Answer text", "gpt-4o", 10, 5, 15, 300L);
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenReturn(result);

        VoicePromptRequest request = new VoicePromptRequest("What's a polite way to decline a meeting?", null);

        VoiceResponse response = voiceAiService.processVoicePrompt(user.getId(), request);

        assertThat(response.language()).isNull();

        ArgumentCaptor<VoiceInteraction> captor = ArgumentCaptor.forClass(VoiceInteraction.class);
        verify(voiceInteractionRepository).save(captor.capture());
        assertThat(captor.getValue().getLanguage()).isNull();
    }

    @Test
    void processVoicePrompt_aiFailure_recordsFailureAnalytics_andPropagatesException_withoutSavingInteraction() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forVoiceCommand(anyString(), any())).thenReturn(prompt);

        AiGenerationException aiFailure = new AiGenerationException("Azure OpenAI request failed after 3 attempt(s)");
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenThrow(aiFailure);

        VoicePromptRequest request = new VoicePromptRequest("Summarize this thread", null);

        assertThatThrownBy(() -> voiceAiService.processVoicePrompt(user.getId(), request))
                .isInstanceOf(AiGenerationException.class);

        verify(usageAnalyticsRecorder).recordFailure(eq(user), eq(RequestType.VOICE_COMMAND), anyString());
        verify(voiceInteractionRepository, never()).save(any());
    }

    @Test
    void getHistory_mapsPageOfInteractionsToVoiceResponses() {
        VoiceInteraction interaction = VoiceInteraction.builder()
                .user(user)
                .transcript("Hello")
                .aiResponse("Hi there")
                .aiModel("gpt-4o")
                .totalTokens(20)
                .latencyMs(400L)
                .build();
        interaction.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 20);
        when(voiceInteractionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(interaction), pageable, 1));

        PageResponse<VoiceResponse> page = voiceAiService.getHistory(user.getId(), pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).aiResponse()).isEqualTo("Hi there");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    private void stubSaveWithGeneratedId() {
        when(voiceInteractionRepository.save(any(VoiceInteraction.class))).thenAnswer(invocation -> {
            VoiceInteraction interaction = invocation.getArgument(0);
            interaction.setId(UUID.randomUUID());
            return interaction;
        });
    }
}
