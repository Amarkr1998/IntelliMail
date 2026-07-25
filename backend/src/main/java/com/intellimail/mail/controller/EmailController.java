package com.intellimail.mail.controller;

import com.intellimail.mail.dto.email.EmailCustomRequest;
import com.intellimail.mail.dto.email.EmailFollowupRequest;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.email.EmailImproveRequest;
import com.intellimail.mail.dto.email.EmailReplyResponse;
import com.intellimail.mail.dto.email.EmailSubjectRequest;
import com.intellimail.mail.dto.email.EmailSummarizeRequest;
import com.intellimail.mail.dto.email.EmailTranslateRequest;
import com.intellimail.mail.dto.email.FileExtractResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.EmailService;
import com.intellimail.mail.service.FileExtractionService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The 20 core AI email-writing features, grouped into 7 endpoints per the
 * platform's REST API contract, plus a file-upload text-extraction endpoint.
 * Every endpoint requires authentication; {@link EmailService} handles
 * persistence, prompt selection, the Azure OpenAI call, and usage-analytics
 * recording.
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "AI-powered email generation, rewriting, translation, summarization and more")
public class EmailController {

    private final EmailService emailService;
    private final FileExtractionService fileExtractionService;

    @PostMapping("/generate")
    @Operation(summary = "Generate an AI reply", description = "Drafts a reply to an existing email thread.")
    public ApiResponse<EmailReplyResponse> generate(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody EmailGenerateRequest request) {
        return ApiResponse.success(emailService.generateReply(principal.getId(), request));
    }

    @PostMapping("/improve")
    @Operation(summary = "Rewrite or correct an email",
            description = "Professional/friendly/formal/casual rewrite, grammar correction, expand, or shorten — selected via the style field.")
    public ApiResponse<EmailReplyResponse> improve(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody EmailImproveRequest request) {
        return ApiResponse.success(emailService.improve(principal.getId(), request));
    }

    @PostMapping("/translate")
    @Operation(summary = "Translate an email", description = "Translates the given content into the requested target language.")
    public ApiResponse<EmailReplyResponse> translate(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody EmailTranslateRequest request) {
        return ApiResponse.success(emailService.translate(principal.getId(), request));
    }

    @PostMapping("/summarize")
    @Operation(summary = "Summarize an email", description = "Produces a concise summary of the given email content.")
    public ApiResponse<EmailReplyResponse> summarize(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody EmailSummarizeRequest request) {
        return ApiResponse.success(emailService.summarize(principal.getId(), request));
    }

    @PostMapping("/subject")
    @Operation(summary = "Generate a subject line", description = "Proposes a concise subject line for the given email body.")
    public ApiResponse<EmailReplyResponse> subject(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody EmailSubjectRequest request) {
        return ApiResponse.success(emailService.subjectLine(principal.getId(), request));
    }

    @PostMapping("/followup")
    @Operation(summary = "Generate a follow-up email", description = "Drafts a polite follow-up for a thread that hasn't received a response.")
    public ApiResponse<EmailReplyResponse> followup(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody EmailFollowupRequest request) {
        return ApiResponse.success(emailService.followup(principal.getId(), request));
    }

    @PostMapping("/custom")
    @Operation(summary = "Compose an email from scratch",
            description = "Backs the Meeting Request, Thank You, Apology, Sales, HR, Marketing, Cold Outreach and fully-Custom-Prompt generators, selected via requestType.")
    public ApiResponse<EmailReplyResponse> custom(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody EmailCustomRequest request) {
        return ApiResponse.success(emailService.custom(principal.getId(), request));
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract text from an uploaded file",
            description = "Accepts PDF, Word, plain text, HTML and other common formats (max 10 MB) and returns the "
                    + "extracted text, ready to feed into any other /api/email/* action as originalContent/content/context. "
                    + "Does not itself call the AI or persist anything.")
    public ApiResponse<FileExtractResponse> extract(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileExtractionService.extractText(file));
    }
}
