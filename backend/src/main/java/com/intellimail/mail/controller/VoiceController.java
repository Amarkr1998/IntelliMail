package com.intellimail.mail.controller;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.voice.VoicePromptRequest;
import com.intellimail.mail.dto.voice.VoiceResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.VoiceAiService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Voice AI: submit a speech-to-text transcript (captured client-side via the
 * Web Speech API) and get back an AI-generated response, with every prompt
 * and response persisted for later review.
 */
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Tag(name = "Voice AI", description = "Speech-to-text voice prompts answered by the AI assistant")
public class VoiceController {

    private final VoiceAiService voiceAiService;

    @PostMapping("/prompt")
    @PreAuthorize("@subscriptionGuard.hasActiveAccess(authentication)")
    @Operation(summary = "Submit a voice prompt", description = "Sends a transcribed voice prompt to the AI and returns/persists its response.")
    public ApiResponse<VoiceResponse> submitPrompt(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody VoicePromptRequest request) {
        return ApiResponse.success(voiceAiService.processVoicePrompt(principal.getId(), request));
    }

    @GetMapping("/history")
    @Operation(summary = "List past voice prompts", description = "Paged, newest first.")
    public ApiResponse<PageResponse<VoiceResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(voiceAiService.getHistory(principal.getId(), pageable));
    }
}
