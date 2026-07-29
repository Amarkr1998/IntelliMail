package com.intellimail.mail.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.agent.export.AgentExportService;
import com.intellimail.mail.agent.reflection.AgentReflectionService;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.agent.AgentTaskRequest;
import com.intellimail.mail.dto.agent.AgentTaskResponse;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.AgentTaskStep;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.AgentTaskStatus;
import com.intellimail.mail.enums.PendingActionType;
import com.intellimail.mail.exception.AgentTaskNotAwaitingConfirmationException;
import com.intellimail.mail.mapper.AgentTaskMapper;
import com.intellimail.mail.mapper.AgentTaskMapperImpl;
import com.intellimail.mail.repository.AgentTaskRepository;
import com.intellimail.mail.repository.AgentTaskStepRepository;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.service.PromptTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient agentChatClient;
    @Mock
    private AgentTaskRepository agentTaskRepository;
    @Mock
    private AgentTaskStepRepository agentTaskStepRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private AgentStepRecorder stepRecorder;
    @Mock
    private AgentPendingActionHolder pendingActionHolder;
    @Mock
    private AgentReflectionService reflectionService;
    @Mock
    private AgentExportService agentExportService;

    private final AgentTaskMapper agentTaskMapper = new AgentTaskMapperImpl();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProperties aiProperties = new AiProperties(0.7, 1024, true, new AiProperties.Retry(3, 500));

    private AgentOrchestrator orchestrator;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        orchestrator = new AgentOrchestrator(agentChatClient, agentTaskRepository, agentTaskStepRepository,
                userRepository, promptTemplateService, stepRecorder, pendingActionHolder, reflectionService,
                agentTaskMapper, objectMapper, aiProperties, agentExportService);

        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        User user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        user.setId(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        // lenient() because the confirm/reject tests precondition their own AgentTask
        // directly (see pendingTask()) rather than going through runTask(), so they
        // never exercise these save() stubs.
        org.mockito.Mockito.lenient().when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> {
            AgentTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(UUID.randomUUID());
            }
            return task;
        });
        org.mockito.Mockito.lenient().when(agentTaskStepRepository.save(any(AgentTaskStep.class))).thenAnswer(invocation -> {
            AgentTaskStep step = invocation.getArgument(0);
            step.setId(UUID.randomUUID());
            return step;
        });
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void runTask_happyPath_completesWithStepsAndNoPendingAction() {
        when(agentChatClient.prompt().system(anyString()).user(anyString()).options(any()).advisors(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Here is the drafted reply.");
        when(reflectionService.reflect(anyString(), anyString()))
                .thenReturn(new AgentReflectionService.ReflectionResult(true, null));
        when(stepRecorder.drain()).thenReturn(List.of(
                new AgentStepRecorder.RecordedStep("generateReply", "in", "Here is the drafted reply.", AgentStepStatus.SUCCESS)));
        when(pendingActionHolder.drain()).thenReturn(Optional.empty());

        AgentTaskResponse response = orchestrator.runTask(userId, organizationId,
                new AgentTaskRequest("Reply to this email", "Original email content", null));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.finalResult()).isEqualTo("Here is the drafted reply.");
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().get(0).toolName()).isEqualTo("generateReply");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.conversationId()).isNotNull();
    }

    @Test
    void runTask_whenReflectionFails_retriesOnceThenAcceptsResult() {
        when(agentChatClient.prompt().system(anyString()).user(anyString()).options(any()).advisors(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Dear [Name], ...")
                .thenReturn("Dear John, here is the reply.");
        when(reflectionService.reflect(anyString(), eq("Dear [Name], ...")))
                .thenReturn(new AgentReflectionService.ReflectionResult(false, "leftover placeholder"));
        when(stepRecorder.drain()).thenReturn(List.of());
        when(pendingActionHolder.drain()).thenReturn(Optional.empty());

        AgentTaskResponse response = orchestrator.runTask(userId, organizationId,
                new AgentTaskRequest("Reply to this email", null, null));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.finalResult()).isEqualTo("Dear John, here is the reply.");
        // reflect() is only called once - the retry's own result is accepted unconditionally.
        verify(reflectionService, org.mockito.Mockito.times(1)).reflect(anyString(), anyString());
    }

    @Test
    void runTask_whenPendingActionProposed_setsAwaitingConfirmationStatus() {
        when(agentChatClient.prompt().system(anyString()).user(anyString()).options(any()).advisors(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Proposed saving this as a template titled 'Decline' - awaiting user confirmation.");
        when(reflectionService.reflect(anyString(), anyString()))
                .thenReturn(new AgentReflectionService.ReflectionResult(true, null));
        when(stepRecorder.drain()).thenReturn(List.of());
        when(pendingActionHolder.drain()).thenReturn(Optional.of(new AgentPendingActionHolder.PendingAction(
                PendingActionType.SAVE_TEMPLATE, Map.of("name", "Decline", "category", "GENERATE_REPLY", "promptText", "..."))));

        AgentTaskResponse response = orchestrator.runTask(userId, organizationId,
                new AgentTaskRequest("Save this as a template", null, null));

        assertThat(response.status()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(response.pendingAction()).isNotNull();
        assertThat(response.pendingAction().actionType()).isEqualTo("SAVE_TEMPLATE");
        assertThat(response.pendingAction().payload().get("name")).isEqualTo("Decline");
    }

    @Test
    void runTask_whenAgentCallThrows_marksTaskFailed_ratherThanPropagating() {
        when(agentChatClient.prompt().system(anyString()).user(anyString()).options(any()).advisors(any(java.util.function.Consumer.class)).call().content())
                .thenThrow(new RuntimeException("model unavailable"));
        when(stepRecorder.drain()).thenReturn(List.of());

        AgentTaskResponse response = orchestrator.runTask(userId, organizationId,
                new AgentTaskRequest("Reply to this email", null, null));

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.finalResult()).contains("model unavailable");
    }

    @Test
    void confirmPendingAction_appliesSaveTemplate_andCompletesTask() throws Exception {
        AgentTask task = pendingTask();
        when(agentTaskRepository.findByIdAndUserId(task.getId(), userId)).thenReturn(Optional.of(task));
        when(agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(task.getId())).thenReturn(List.of());
        when(promptTemplateService.createTemplate(eq(userId), eq(organizationId), any()))
                .thenReturn(new PromptTemplateResponse(UUID.randomUUID(), "Decline", null,
                        com.intellimail.mail.enums.RequestType.GENERATE_REPLY, "...", null, false, null, null, null));

        AgentTaskResponse response = orchestrator.confirmPendingAction(userId, organizationId, task.getId());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.pendingAction()).isNull();
        ArgumentCaptor<com.intellimail.mail.dto.template.PromptTemplateRequest> captor =
                ArgumentCaptor.forClass(com.intellimail.mail.dto.template.PromptTemplateRequest.class);
        verify(promptTemplateService).createTemplate(eq(userId), eq(organizationId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Decline");
    }

    @Test
    void confirmPendingAction_whenTaskHasNoPendingAction_throws() {
        AgentTask task = AgentTask.builder()
                .user(userRepository.getReferenceById(userId))
                .status(AgentTaskStatus.COMPLETED)
                .goal("goal")
                .conversationId(UUID.randomUUID())
                .build();
        task.setId(UUID.randomUUID());
        when(agentTaskRepository.findByIdAndUserId(task.getId(), userId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> orchestrator.confirmPendingAction(userId, organizationId, task.getId()))
                .isInstanceOf(AgentTaskNotAwaitingConfirmationException.class);
        verify(promptTemplateService, never()).createTemplate(any(), any(), any());
    }

    @Test
    void rejectPendingAction_marksRejected_withoutCallingPromptTemplateService() {
        AgentTask task = pendingTask();
        when(agentTaskRepository.findByIdAndUserId(task.getId(), userId)).thenReturn(Optional.of(task));
        when(agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(task.getId())).thenReturn(List.of());

        AgentTaskResponse response = orchestrator.rejectPendingAction(userId, task.getId());

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.pendingAction()).isNull();
        verify(promptTemplateService, never()).createTemplate(any(), any(), any());
    }

    private AgentTask pendingTask() {
        try {
            AgentTask task = AgentTask.builder()
                    .user(userRepository.getReferenceById(userId))
                    .organizationId(organizationId)
                    .status(AgentTaskStatus.AWAITING_CONFIRMATION)
                    .goal("Save this as a template")
                    .finalResult("Proposed saving this as a template titled 'Decline'.")
                    .conversationId(UUID.randomUUID())
                    .pendingActionType(PendingActionType.SAVE_TEMPLATE)
                    .pendingActionPayload(objectMapper.writeValueAsString(
                            Map.of("name", "Decline", "description", "", "category", "GENERATE_REPLY",
                                    "promptText", "Please decline politely", "systemPrompt", "", "isPublic", false)))
                    .build();
            task.setId(UUID.randomUUID());
            return task;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
