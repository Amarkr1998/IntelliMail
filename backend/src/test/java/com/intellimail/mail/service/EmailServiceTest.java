package com.intellimail.mail.service;

import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.email.EmailImproveRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSummarizeRequest;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.PromptTemplate;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.AiGenerationException;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.logging.UsageAnalyticsRecorder;
import com.intellimail.mail.mapper.EmailMapper;
import com.intellimail.mail.mapper.EmailMapperImpl;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.prompt.PromptFactory;
import com.intellimail.mail.repository.EmailRequestRepository;
import com.intellimail.mail.repository.GeneratedReplyRepository;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRequestRepository emailRequestRepository;
    @Mock
    private GeneratedReplyRepository generatedReplyRepository;
    @Mock
    private PromptTemplateRepository promptTemplateRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UsageAnalyticsRecorder usageAnalyticsRecorder;
    @Mock
    private PromptFactory promptFactory;
    @Mock
    private AzureOpenAiClient azureOpenAiClient;

    private final EmailMapper emailMapper = new EmailMapperImpl();
    private final AiProperties aiProperties = new AiProperties(0.7, 1024, true, new AiProperties.Retry(3, 500));

    private EmailService emailService;
    private User user;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
                emailRequestRepository, generatedReplyRepository, promptTemplateRepository, userRepository,
                usageAnalyticsRecorder, promptFactory, azureOpenAiClient, aiProperties, emailMapper);

        user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());

        // Every EmailService method now runs its prompt through withReferenceContext();
        // this test suite doesn't exercise reference-context behavior itself (see
        // EmailServiceReferenceContextTest), so just echo the prompt back unchanged.
        // lenient() because not every test path reaches this call (e.g. the
        // inaccessible-prompt-template test throws before any prompt is built).
        lenient().when(promptFactory.withReferenceContext(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generateReply_happyPath_savesRequestAndReplyAndRecordsSuccess() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        stubEmailRequestSaveWithGeneratedId();
        stubGeneratedReplySaveWithGeneratedId();

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forGenerateReply(anyString(), any(), any())).thenReturn(prompt);

        AiGenerationResult result = new AiGenerationResult("Generated reply text", "gpt-4o", 50, 30, 80, 900L);
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenReturn(result);

        EmailGenerateRequest request = new EmailGenerateRequest("Original email content", "Keep it short", null, null);

        EmailReplyResponse response = emailService.generateReply(user.getId(), request);

        assertThat(response.content()).isEqualTo("Generated reply text");
        assertThat(response.requestType()).isEqualTo(RequestType.GENERATE_REPLY);
        assertThat(response.totalTokens()).isEqualTo(80);
        verify(usageAnalyticsRecorder).recordSuccess(user, RequestType.GENERATE_REPLY, 80, 900L);
        verify(promptTemplateRepository, never()).findById(any());
    }

    @Test
    void generateReply_withInaccessiblePromptTemplate_throwsResourceNotFound_andNeverCallsAi() {
        UUID templateId = UUID.randomUUID();
        User otherOwner = User.builder().fullName("Other").email("other@intellimail.com").password("hashed").build();
        otherOwner.setId(UUID.randomUUID());
        PromptTemplate privateTemplate = PromptTemplate.builder()
                .name("Private")
                .category(RequestType.GENERATE_REPLY)
                .promptText("...")
                .owner(otherOwner)
                .isPublic(false)
                .build();
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(privateTemplate));

        EmailGenerateRequest request = new EmailGenerateRequest("content", null, templateId, null);

        assertThatThrownBy(() -> emailService.generateReply(user.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(azureOpenAiClient);
        verify(emailRequestRepository, never()).save(any());
    }

    @Test
    void improve_persistsRequestWithRequestedStyleAsRequestType() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        stubEmailRequestSaveWithGeneratedId();
        stubGeneratedReplySaveWithGeneratedId();

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forRewrite(eq(RequestType.GRAMMAR_CORRECTION), anyString(), any())).thenReturn(prompt);

        AiGenerationResult result = new AiGenerationResult("Fixed text", "gpt-4o", 20, 10, 30, 400L);
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenReturn(result);

        EmailImproveRequest request = new EmailImproveRequest("Some typo-ridden text", RequestType.GRAMMAR_CORRECTION, null);

        EmailReplyResponse response = emailService.improve(user.getId(), request);

        assertThat(response.requestType()).isEqualTo(RequestType.GRAMMAR_CORRECTION);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestType()).isEqualTo(RequestType.GRAMMAR_CORRECTION);
    }

    @Test
    void aiFailure_recordsFailureAnalytics_andPropagatesException_withoutSavingReply() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        stubEmailRequestSaveWithGeneratedId();

        PreparedPrompt prompt = new PreparedPrompt("system", "user-turn");
        when(promptFactory.forSummarize(anyString())).thenReturn(prompt);

        AiGenerationException aiFailure = new AiGenerationException("Azure OpenAI request failed after 3 attempt(s)");
        when(azureOpenAiClient.generate(eq(prompt), anyDouble(), anyInt())).thenThrow(aiFailure);

        EmailSummarizeRequest request = new EmailSummarizeRequest("Some long email content to summarize", null);

        assertThatThrownBy(() -> emailService.summarize(user.getId(), request))
                .isInstanceOf(AiGenerationException.class);

        verify(usageAnalyticsRecorder).recordFailure(eq(user), eq(RequestType.SUMMARIZE), anyString());
        verify(generatedReplyRepository, never()).save(any());
    }

    private void stubEmailRequestSaveWithGeneratedId() {
        when(emailRequestRepository.save(any(EmailRequest.class))).thenAnswer(invocation -> {
            EmailRequest emailRequest = invocation.getArgument(0);
            emailRequest.setId(UUID.randomUUID());
            return emailRequest;
        });
    }

    private void stubGeneratedReplySaveWithGeneratedId() {
        when(generatedReplyRepository.save(any(GeneratedReply.class))).thenAnswer(invocation -> {
            GeneratedReply reply = invocation.getArgument(0);
            reply.setId(UUID.randomUUID());
            return reply;
        });
    }
}
