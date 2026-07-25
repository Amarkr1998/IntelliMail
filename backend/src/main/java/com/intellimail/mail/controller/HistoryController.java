package com.intellimail.mail.controller;

import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.email.EmailHistoryResponse;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.EmailService;
import com.intellimail.mail.service.HistoryService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * History browsing/deletion, plus Reply Regeneration and Favorite Replies —
 * the two core features that operate on an existing history entry rather
 * than creating a new one.
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "AI response history, regeneration and favorites")
public class HistoryController {

    private final HistoryService historyService;
    private final EmailService emailService;

    @GetMapping
    @Operation(summary = "List AI response history", description = "Paged, newest first; each entry includes every regeneration attempt.")
    public ApiResponse<PageResponse<EmailHistoryResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(historyService.getHistory(principal.getId(), pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a history entry", description = "Deletes an EmailRequest and cascades to its replies/feedback.")
    public ApiResponse<Void> deleteHistoryEntry(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        historyService.deleteHistoryEntry(principal.getId(), id);
        return ApiResponse.success("History entry deleted", null);
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerate a reply", description = "Re-runs the AI call for an existing request, adding a new attempt without discarding prior ones.")
    public ApiResponse<EmailReplyResponse> regenerate(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.success(emailService.regenerate(principal.getId(), id));
    }

    @PatchMapping("/replies/{replyId}/favorite")
    @Operation(summary = "Favorite or unfavorite a reply")
    public ApiResponse<EmailReplyResponse> setFavorite(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable UUID replyId,
                                                        @RequestParam(defaultValue = "true") boolean favorite) {
        return ApiResponse.success(historyService.setFavorite(principal.getId(), replyId, favorite));
    }
}
