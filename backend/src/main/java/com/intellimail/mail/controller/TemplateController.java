package com.intellimail.mail.controller;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.PromptTemplateService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Reusable AI prompt templates")
public class TemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    @Operation(summary = "List visible prompt templates", description = "Returns the caller's own templates plus every public/system template.")
    public ApiResponse<PageResponse<PromptTemplateResponse>> getTemplates(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(promptTemplateService.getTemplates(principal.getId(), pageable));
    }

    @PostMapping
    @Operation(summary = "Create a prompt template")
    public ResponseEntity<ApiResponse<PromptTemplateResponse>> createTemplate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PromptTemplateRequest request) {
        PromptTemplateResponse response = promptTemplateService.createTemplate(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Template created", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prompt template", description = "Only the template's owner may update it.")
    public ApiResponse<PromptTemplateResponse> updateTemplate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.success(promptTemplateService.updateTemplate(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prompt template", description = "Only the template's owner may delete it.")
    public ApiResponse<Void> deleteTemplate(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        promptTemplateService.deleteTemplate(principal.getId(), id);
        return ApiResponse.success("Template deleted", null);
    }
}
