package com.intellimail.mail.controller;

import com.intellimail.mail.agent.AgentOrchestrator;
import com.intellimail.mail.dto.agent.AgentTaskRequest;
import com.intellimail.mail.dto.agent.AgentTaskResponse;
import com.intellimail.mail.dto.agent.AgentTaskSummaryResponse;
import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The AI agent: chains the existing email-generation tools together for
 * multi-step goals, with a mandatory confirm/reject step before the one
 * mutating action (saving a template) is ever actually persisted.
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "AI agent tool-calling/planner over the existing email-generation capabilities")
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;

    @PostMapping("/tasks")
    @PreAuthorize("@subscriptionGuard.hasActiveAccess(authentication)")
    @Operation(summary = "Run an agent task", description = "Submits a goal for the agent to work on, chaining tools as needed. "
            + "Pass back a prior response's conversationId to continue that conversation.")
    public ApiResponse<AgentTaskResponse> runTask(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody AgentTaskRequest request) {
        return ApiResponse.success(agentOrchestrator.runTask(principal.getId(), principal.getOrganizationId(), request));
    }

    @PostMapping("/tasks/{id}/confirm")
    @Operation(summary = "Confirm a pending action", description = "Executes the one action the agent proposed (e.g. saving a template).")
    public ApiResponse<AgentTaskResponse> confirm(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.success(agentOrchestrator.confirmPendingAction(principal.getId(), principal.getOrganizationId(), id));
    }

    @PostMapping("/tasks/{id}/reject")
    @Operation(summary = "Reject a pending action", description = "Discards the action the agent proposed without executing it.")
    public ApiResponse<AgentTaskResponse> reject(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.success(agentOrchestrator.rejectPendingAction(principal.getId(), id));
    }

    @GetMapping("/tasks")
    @Operation(summary = "List agent task history", description = "Paged, newest first.")
    public ApiResponse<PageResponse<AgentTaskSummaryResponse>> listTasks(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(agentOrchestrator.listTasks(principal.getId(), pageable));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get one agent task", description = "Full detail including its steps and any pending action.")
    public ApiResponse<AgentTaskResponse> getTask(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.success(agentOrchestrator.getTask(principal.getId(), id));
    }
}
