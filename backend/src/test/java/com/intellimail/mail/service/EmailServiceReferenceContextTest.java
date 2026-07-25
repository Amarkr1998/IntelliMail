package com.intellimail.mail.service;

import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.GeneratedReply;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.logging.UsageAnalyticsRecorder;
import com.intellimail.mail.mapper.EmailMapper;
import com.intellimail.mail.mapper.EmailMapperImpl;
import com.intellimail.mail.prompt.PreparedPrompt;
import com.intellimail.mail.prompt.PromptFactory;
import com.intellimail.mail.prompt.SystemPromptCatalog;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the actual wiring EmailServiceTest deliberately stubs out: that
 * {@code referenceContext} (e.g. text extracted from an uploaded file) is
 * persisted on the {@code EmailRequest}, actually reaches the AI as a
 * clearly-labeled reference block via the real {@link PromptFactory}, is
 * kept entirely separate from the field being acted on, and survives Reply
 * Regeneration.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceReferenceContextTest {

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
    private AzureOpenAiClient azureOpenAiClient;

    private final PromptFactory promptFactory = new PromptFactory(new SystemPromptCatalog());
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
    }

    @Test
    void generateReply_withReferenceContext_persistsIt_andSendsItToAiAsALabeledBlock() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(emailRequestRepository.save(any(EmailRequest.class))).thenAnswer(invocation -> {
            EmailRequest emailRequest = invocation.getArgument(0);
            emailRequest.setId(UUID.randomUUID());
            return emailRequest;
        });
        when(generatedReplyRepository.save(any(GeneratedReply.class))).thenAnswer(invocation -> {
            GeneratedReply reply = invocation.getArgument(0);
            reply.setId(UUID.randomUUID());
            return reply;
        });

        ArgumentCaptor<PreparedPrompt> promptCaptor = ArgumentCaptor.forClass(PreparedPrompt.class);
        when(azureOpenAiClient.generate(promptCaptor.capture(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("A reply", "gpt-4o", 10, 5, 15, 100L));

        EmailGenerateRequest request = new EmailGenerateRequest(
                "Can we confirm the meeting?", null, null,
                "Our pricing: Basic $10/mo, Pro $30/mo, Enterprise custom.");

        emailService.generateReply(user.getId(), request);

        String sentUserPrompt = promptCaptor.getValue().userPrompt();
        assertThat(sentUserPrompt).contains("Can we confirm the meeting?");
        assertThat(sentUserPrompt).contains("Reference material");
        assertThat(sentUserPrompt).contains("Our pricing: Basic $10/mo, Pro $30/mo, Enterprise custom.");

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailRequestRepository).save(emailRequestCaptor.capture());
        assertThat(emailRequestCaptor.getValue().getOriginalContent()).isEqualTo("Can we confirm the meeting?");
        assertThat(emailRequestCaptor.getValue().getReferenceContext())
                .isEqualTo("Our pricing: Basic $10/mo, Pro $30/mo, Enterprise custom.");
    }

    @Test
    void generateReply_withoutReferenceContext_sendsNoReferenceBlock() {
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(emailRequestRepository.save(any(EmailRequest.class))).thenAnswer(invocation -> {
            EmailRequest emailRequest = invocation.getArgument(0);
            emailRequest.setId(UUID.randomUUID());
            return emailRequest;
        });
        when(generatedReplyRepository.save(any(GeneratedReply.class))).thenAnswer(invocation -> {
            GeneratedReply reply = invocation.getArgument(0);
            reply.setId(UUID.randomUUID());
            return reply;
        });

        ArgumentCaptor<PreparedPrompt> promptCaptor = ArgumentCaptor.forClass(PreparedPrompt.class);
        when(azureOpenAiClient.generate(promptCaptor.capture(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("A reply", "gpt-4o", 10, 5, 15, 100L));

        EmailGenerateRequest request = new EmailGenerateRequest("Can we confirm the meeting?", null, null, null);

        emailService.generateReply(user.getId(), request);

        assertThat(promptCaptor.getValue().userPrompt()).doesNotContain("Reference material");
    }

    @Test
    void regenerate_reusesThePersistedReferenceContext() {
        EmailRequest existingRequest = EmailRequest.builder()
                .user(user)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent("Can we confirm the meeting?")
                .referenceContext("Our pricing: Basic $10/mo.")
                .build();
        existingRequest.setId(UUID.randomUUID());

        when(emailRequestRepository.findById(existingRequest.getId())).thenReturn(Optional.of(existingRequest));
        when(generatedReplyRepository.findMaxAttemptNumber(existingRequest.getId())).thenReturn(1);
        when(generatedReplyRepository.save(any(GeneratedReply.class))).thenAnswer(invocation -> {
            GeneratedReply reply = invocation.getArgument(0);
            reply.setId(UUID.randomUUID());
            return reply;
        });

        ArgumentCaptor<PreparedPrompt> promptCaptor = ArgumentCaptor.forClass(PreparedPrompt.class);
        when(azureOpenAiClient.generate(promptCaptor.capture(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("A second reply", "gpt-4o", 10, 5, 15, 100L));

        emailService.regenerate(user.getId(), existingRequest.getId());

        assertThat(promptCaptor.getValue().userPrompt()).contains("Our pricing: Basic $10/mo.");

        ArgumentCaptor<GeneratedReply> replyCaptor = ArgumentCaptor.forClass(GeneratedReply.class);
        verify(generatedReplyRepository).save(replyCaptor.capture());
        assertThat(replyCaptor.getValue().getAttemptNumber()).isEqualTo(2);
    }
}
