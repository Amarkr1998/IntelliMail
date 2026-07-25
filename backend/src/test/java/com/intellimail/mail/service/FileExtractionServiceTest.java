package com.intellimail.mail.service;

import com.intellimail.mail.dto.email.FileExtractResponse;
import com.intellimail.mail.exception.FileProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileExtractionServiceTest {

    private final FileExtractionService service = new FileExtractionService();

    @Test
    void extractText_fromPlainTextFile_returnsContentVerbatim() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain",
                "Hi team, are we still on for the product demo this Thursday at 10am?".getBytes(StandardCharsets.UTF_8));

        FileExtractResponse response = service.extractText(file);

        assertThat(response.fileName()).isEqualTo("note.txt");
        assertThat(response.content()).contains("product demo this Thursday at 10am");
        assertThat(response.truncated()).isFalse();
        assertThat(response.characterCount()).isEqualTo(response.content().length());
    }

    @Test
    void extractText_withEmptyFile_throwsFileProcessingException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void extractText_withNullFile_throwsFileProcessingException() {
        assertThatThrownBy(() -> service.extractText(null))
                .isInstanceOf(FileProcessingException.class);
    }

    @Test
    void extractText_withWhitespaceOnlyContent_throwsFileProcessingException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blank.txt", "text/plain", "   \n\n   ".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("No readable text");
    }

    @Test
    void extractText_truncatesContentLongerThan20000Characters() {
        String longText = "A".repeat(25_000);
        MockMultipartFile file = new MockMultipartFile(
                "file", "long.txt", "text/plain", longText.getBytes(StandardCharsets.UTF_8));

        FileExtractResponse response = service.extractText(file);

        assertThat(response.truncated()).isTrue();
        assertThat(response.content()).hasSize(20_000);
        assertThat(response.characterCount()).isEqualTo(20_000);
    }
}
