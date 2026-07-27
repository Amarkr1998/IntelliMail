package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.dto.voice.VoicePromptRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full register -> authenticate -> submit-voice-prompt ->
 * history path through real Spring Security + JPA/H2, mocking only
 * {@link AzureOpenAiClient} — the one thing that actually talks to an
 * external service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private AzureOpenAiClient azureOpenAiClient;

    @BeforeEach
    void ensureDefaultRoleExists() {
        roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build()));
    }

    @Test
    void submitPrompt_endToEnd_returnsAiResponseAndPersistsHistory() throws Exception {
        when(azureOpenAiClient.generate(any(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("Sure, Tuesday at 3pm works.", "gpt-4o", 30, 20, 50, 600L));

        String accessToken = registerAndGetAccessToken("voice.integration@intellimail.com");

        mockMvc.perform(post("/api/voice/prompt")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VoicePromptRequest("Reply saying Tuesday at 3pm works for me", "English (US)"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiResponse").value("Sure, Tuesday at 3pm works."))
                .andExpect(jsonPath("$.data.transcript").value("Reply saying Tuesday at 3pm works for me"))
                .andExpect(jsonPath("$.data.totalTokens").value(50));

        mockMvc.perform(get("/api/voice/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].aiResponse").value("Sure, Tuesday at 3pm works."))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void submitPrompt_withBlankTranscript_isRejectedWithValidationError() throws Exception {
        String accessToken = registerAndGetAccessToken("voice.validation@intellimail.com");

        mockMvc.perform(post("/api/voice/prompt")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoicePromptRequest("  ", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitPrompt_withoutToken_isRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/voice/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoicePromptRequest("Hello", null))))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Integration Test User", email, "password123");
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).at("/data/accessToken").asText();
    }
}
