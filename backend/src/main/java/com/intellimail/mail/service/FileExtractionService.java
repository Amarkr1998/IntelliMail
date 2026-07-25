package com.intellimail.mail.service;

import com.intellimail.mail.dto.email.FileExtractResponse;
import com.intellimail.mail.exception.FileProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extracts plain text from an uploaded file of (almost) any common format —
 * PDF, Word, plain text, HTML, RTF, and more — via Apache Tika's
 * {@code AutoDetectParser} (wrapped by the {@link Tika} facade), so callers
 * never need to know or check the file's actual type.
 *
 * <p>This service does exactly one thing: bytes in, text out. It has no
 * awareness of {@code EmailRequest}/AI generation at all — the extracted text
 * is meant to be handed to any existing {@code /api/email/*} endpoint by the
 * caller, reusing that pipeline's prompt engineering, persistence, and
 * analytics unchanged rather than duplicating it here.
 */
@Service
@Slf4j
public class FileExtractionService {

    /** Matches the existing @Size(max = 20_000) cap on originalContent/content across /api/email/* DTOs. */
    private static final int MAX_RETURNED_CHARACTERS = 20_000;

    /** Coarse safety net against pathological extraction blow-up, independent of the final returned cap above. */
    private static final int TIKA_INTERNAL_LIMIT = 500_000;

    private final Tika tika;

    public FileExtractionService() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(TIKA_INTERNAL_LIMIT);
    }

    public FileExtractResponse extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("Uploaded file is empty");
        }

        String rawText;
        try {
            rawText = tika.parseToString(file.getInputStream());
        } catch (Exception ex) {
            log.warn("Failed to extract text from uploaded file '{}': {}", file.getOriginalFilename(), ex.getMessage());
            throw new FileProcessingException(
                    "Could not read this file. Supported formats include PDF, Word, plain text, and more.", ex);
        }

        String trimmed = rawText == null ? "" : rawText.strip();
        if (trimmed.isEmpty()) {
            throw new FileProcessingException("No readable text was found in this file");
        }

        boolean truncated = trimmed.length() > MAX_RETURNED_CHARACTERS;
        String content = truncated ? trimmed.substring(0, MAX_RETURNED_CHARACTERS) : trimmed;

        return new FileExtractResponse(file.getOriginalFilename(), content, content.length(), truncated);
    }
}
