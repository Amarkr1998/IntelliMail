package com.intellimail.mail.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.agent.export.AgentExportService;
import com.intellimail.mail.agent.prompt.AgentSystemPrompts;
import com.intellimail.mail.agent.reflection.AgentReflectionService;
import com.intellimail.mail.config.AiProperties;
import com.intellimail.mail.dto.agent.AgentStepResponse;
import com.intellimail.mail.dto.agent.AgentTaskRequest;
import com.intellimail.mail.dto.agent.AgentTaskResponse;
import com.intellimail.mail.dto.agent.AgentTaskSummaryResponse;
import com.intellimail.mail.dto.agent.PendingActionResponse;
import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.AgentTaskStep;
import com.intellimail.mail.enums.AgentTaskStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.exception.AgentTaskNotAwaitingConfirmationException;
import com.intellimail.mail.exception.ResourceNotFoundException;
import com.intellimail.mail.mapper.AgentTaskMapper;
import com.intellimail.mail.repository.AgentTaskRepository;
import com.intellimail.mail.repository.AgentTaskStepRepository;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Entry point for the AI agent: runs the tool-calling/planning loop for one
 * goal, persists the resulting task + steps, and gates the one mutating
 * action (saving a template) behind an explicit confirm/reject call rather
 * than ever executing it from inside the model loop itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final @Qualifier("agentChatClient") ChatClient agentChatClient;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTaskStepRepository agentTaskStepRepository;
    private final UserRepository userRepository;
    private final PromptTemplateService promptTemplateService;
    private final AgentStepRecorder stepRecorder;
    private final AgentPendingActionHolder pendingActionHolder;
    private final AgentReflectionService reflectionService;
    private final AgentTaskMapper agentTaskMapper;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final AgentExportService agentExportService;

    @Transactional
    public AgentTaskResponse runTask(UUID userId, UUID organizationId, AgentTaskRequest request) {
        UUID conversationId = request.conversationId() != null ? request.conversationId() : UUID.randomUUID();

        AgentTask task = agentTaskRepository.save(AgentTask.builder()
                .user(userRepository.getReferenceById(userId))
                .organizationId(organizationId)
                .goal(request.goal())
                .status(AgentTaskStatus.IN_PROGRESS)
                .conversationId(conversationId)
                .build());

        AgentExecutionContext.set(userId, organizationId, task.getId());
        try {
            String userMessage = buildUserMessage(request.goal(), request.context());

            String result;
            try {
                result = callAgent(userMessage, conversationId);
                AgentReflectionService.ReflectionResult reflection = reflectionService.reflect(request.goal(), result);
                if (!reflection.pass()) {
                    String retryMessage = userMessage + "\n\nYour previous attempt had an issue: "
                            + reflection.reason() + ". Please address this and try again.";
                    result = callAgent(retryMessage, conversationId);
                }
            } catch (RuntimeException ex) {
                log.error("Agent task {} failed", task.getId(), ex);
                List<AgentTaskStep> failedSteps = persistSteps(task, stepRecorder.drain());
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResult("The agent could not complete this task: " + ex.getMessage());
                agentTaskRepository.save(task);
                return buildResponse(task, failedSteps);
            }

            List<AgentTaskStep> steps = persistSteps(task, stepRecorder.drain());

            Optional<AgentPendingActionHolder.PendingAction> pending = pendingActionHolder.drain();
            if (pending.isPresent()) {
                task.setStatus(AgentTaskStatus.AWAITING_CONFIRMATION);
                task.setPendingActionType(pending.get().type());
                task.setPendingActionPayload(writeJson(pending.get().payload()));
            } else {
                task.setStatus(AgentTaskStatus.COMPLETED);
            }
            task.setFinalResult(result);
            agentTaskRepository.save(task);

            return buildResponse(task, steps);
        } finally {
            AgentExecutionContext.clear();
        }
    }

    @Transactional
    public AgentTaskResponse confirmPendingAction(UUID userId, UUID organizationId, UUID taskId) {
        AgentTask task = getOwnedTask(userId, taskId);
        requireAwaitingConfirmation(task, "confirm");

        applyPendingAction(userId, organizationId, task);

        task.setStatus(AgentTaskStatus.COMPLETED);
        task.setFinalResult(task.getFinalResult() + "\n\n[Template saved.]");
        task.setPendingActionType(null);
        task.setPendingActionPayload(null);
        agentTaskRepository.save(task);

        return buildResponse(task, agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(taskId));
    }

    @Transactional
    public AgentTaskResponse rejectPendingAction(UUID userId, UUID taskId) {
        AgentTask task = getOwnedTask(userId, taskId);
        requireAwaitingConfirmation(task, "reject");

        task.setStatus(AgentTaskStatus.REJECTED);
        task.setFinalResult(task.getFinalResult() + "\n\n[Proposal rejected by user.]");
        task.setPendingActionType(null);
        task.setPendingActionPayload(null);
        agentTaskRepository.save(task);

        return buildResponse(task, agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(taskId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentTaskSummaryResponse> listTasks(UUID userId, Pageable pageable) {
        Page<AgentTask> page = agentTaskRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, agentTaskMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public AgentTaskResponse getTask(UUID userId, UUID taskId) {
        AgentTask task = getOwnedTask(userId, taskId);
        List<AgentTaskStep> steps = agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(taskId);
        return buildResponse(task, steps);
    }

    @Transactional(readOnly = true)
    public byte[] exportTaskAsPdf(UUID userId, UUID taskId) {
        AgentTask task = getOwnedTask(userId, taskId);
        List<AgentTaskStep> steps = agentTaskStepRepository.findByAgentTaskIdOrderByStepNumberAsc(taskId);
        return agentExportService.renderTaskAsPdf(task, steps);
    }

    private String callAgent(String userMessage, UUID conversationId) {
        return agentChatClient.prompt()
                .system(AgentSystemPrompts.ORCHESTRATION_SYSTEM_PROMPT)
                .user(userMessage)
                .options(aiProperties.toChatOptions())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                .call()
                .content();
    }

    private String buildUserMessage(String goal, String context) {
        return (context == null || context.isBlank()) ? goal : goal + "\n\nContext:\n" + context;
    }

    private void requireAwaitingConfirmation(AgentTask task, String action) {
        if (task.getStatus() != AgentTaskStatus.AWAITING_CONFIRMATION || task.getPendingActionType() == null) {
            throw new AgentTaskNotAwaitingConfirmationException(
                    "This task has no pending action to " + action);
        }
    }

    private void applyPendingAction(UUID userId, UUID organizationId, AgentTask task) {
        switch (task.getPendingActionType()) {
            case SAVE_TEMPLATE -> {
                Map<String, Object> payload = readJson(task.getPendingActionPayload());
                RequestType category = RequestType.valueOf(String.valueOf(payload.get("category")));
                PromptTemplateRequest request = new PromptTemplateRequest(
                        (String) payload.get("name"),
                        blankToNull((String) payload.get("description")),
                        category,
                        (String) payload.get("promptText"),
                        blankToNull((String) payload.get("systemPrompt")),
                        Boolean.TRUE.equals(payload.get("isPublic")));
                promptTemplateService.createTemplate(userId, organizationId, request);
            }
        }
    }

    private AgentTask getOwnedTask(UUID userId, UUID taskId) {
        return agentTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentTask", taskId));
    }

    private List<AgentTaskStep> persistSteps(AgentTask task, List<AgentStepRecorder.RecordedStep> recorded) {
        List<AgentTaskStep> saved = new ArrayList<>();
        int stepNumber = 1;
        for (AgentStepRecorder.RecordedStep step : recorded) {
            saved.add(agentTaskStepRepository.save(AgentTaskStep.builder()
                    .agentTask(task)
                    .stepNumber(stepNumber++)
                    .toolName(step.toolName())
                    .inputSummary(step.inputSummary())
                    .outputSummary(step.outputSummary())
                    .status(step.status())
                    .build()));
        }
        return saved;
    }

    private AgentTaskResponse buildResponse(AgentTask task, List<AgentTaskStep> steps) {
        List<AgentStepResponse> stepResponses = steps.stream().map(agentTaskMapper::toStepResponse).toList();
        PendingActionResponse pendingAction = task.getPendingActionType() != null
                ? new PendingActionResponse(task.getPendingActionType().name(), readJson(task.getPendingActionPayload()))
                : null;
        return new AgentTaskResponse(task.getId(), task.getStatus().name(), task.getFinalResult(),
                stepResponses, pendingAction, task.getConversationId());
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize pending action payload", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize pending action payload", ex);
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
