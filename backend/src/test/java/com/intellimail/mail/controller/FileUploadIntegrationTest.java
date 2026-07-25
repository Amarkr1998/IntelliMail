package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the upload endpoint works through the real security filter chain
 * (requires a valid access token, same as every other /api/email/* route)
 * and that a plain-text file round-trips correctly. AzureOpenAiClient is
 * never invoked by this endpoint, so unlike EmailGenerationIntegrationTest,
 * nothing needs to be mocked here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Upload Test User", "upload.test." + UUID.randomUUID() + "@intellimail.local", "password123");
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseBody).at("/data/accessToken").asText();
    }

    @Test
    void extract_withPlainTextFile_returnsExtractedContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "meeting-notes.txt", "text/plain",
                "Please confirm the product demo for Thursday at 10am.".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/email/extract")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("meeting-notes.txt"))
                .andExpect(jsonPath("$.data.content").value("Please confirm the product demo for Thursday at 10am."))
                .andExpect(jsonPath("$.data.truncated").value(false));
    }

    @Test
    void extract_withoutToken_isRejectedWithUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/email/extract").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extract_withEmptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/email/extract")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
