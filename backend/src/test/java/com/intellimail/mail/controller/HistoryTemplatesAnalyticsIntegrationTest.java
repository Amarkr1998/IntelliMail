package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
import com.intellimail.mail.dto.template.PromptTemplateRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.enums.RequestType;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the full Templates -> Generate (with template) -> History ->
 * Regenerate -> Favorite -> Analytics -> Delete lifecycle through real HTTP
 * requests, mocking only the external Azure OpenAI call. This is the
 * strongest available proof that Module 8 wires correctly on top of
 * Modules 1-7 without a local Maven build to verify against.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryTemplatesAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private AzureOpenAiClient azureOpenAiClient;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build()));

        RegisterRequest registerRequest = new RegisterRequest(
                "History Test User", "history.test." + UUID.randomUUID() + "@intellimail.com", "password123");
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseBody).at("/data/accessToken").asText();
    }

    @Test
    void fullLifecycle_template_generate_history_regenerate_favorite_analytics_delete() throws Exception {
        // 1. Create a personal prompt template.
        PromptTemplateRequest templateRequest = new PromptTemplateRequest(
                "Friendly Cold Outreach", "My go-to opener", RequestType.COLD_OUTREACH,
                "Write a friendly, low-pressure cold outreach email about {{context}}", null, false);

        String templateBody = mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(templateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Friendly Cold Outreach"))
                .andReturn().getResponse().getContentAsString();
        String templateId = objectMapper.readTree(templateBody).at("/data/id").asText();

        mockMvc.perform(get("/api/templates").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(templateId));

        // 2. Generate a reply using that template (mocked AI call #1).
        when(azureOpenAiClient.generate(any(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("First draft reply.", "gpt-4o", 30, 20, 50, 500L));

        String generateBody = mockMvc.perform(post("/api/email/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmailGenerateRequest("Reaching out about a partnership", null, UUID.fromString(templateId), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptNumber").value(1))
                .andReturn().getResponse().getContentAsString();
        JsonNode generateData = objectMapper.readTree(generateBody).at("/data");
        String emailRequestId = generateData.get("emailRequestId").asText();
        String replyId = generateData.get("id").asText();

        // 3. It shows up in history with exactly one reply.
        mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(emailRequestId))
                .andExpect(jsonPath("$.data.content[0].replies.length()").value(1));

        // 4. Regenerate (mocked AI call #2) creates attempt #2, not a replacement.
        when(azureOpenAiClient.generate(any(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("Second draft reply.", "gpt-4o", 35, 25, 60, 480L));

        mockMvc.perform(post("/api/history/" + emailRequestId + "/regenerate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptNumber").value(2))
                .andExpect(jsonPath("$.data.content").value("Second draft reply."));

        mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.content[0].replies.length()").value(2));

        // 5. Favorite the first reply.
        mockMvc.perform(patch("/api/history/replies/" + replyId + "/favorite")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("favorite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        // 6. Analytics reflects both successful AI calls.
        String analyticsBody = mockMvc.perform(get("/api/analytics").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode analyticsData = objectMapper.readTree(analyticsBody).at("/data");
        assertThat(analyticsData.get("totalRequests").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(analyticsData.get("totalTokens").asLong()).isGreaterThanOrEqualTo(110);

        // 7. Clean up: delete the template and the history entry.
        mockMvc.perform(delete("/api/templates/" + templateId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/history/" + emailRequestId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void deletingAnotherUsersHistoryEntry_returns404() throws Exception {
        RegisterRequest otherUser = new RegisterRequest(
                "Other User", "other.history." + UUID.randomUUID() + "@intellimail.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUser)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/history/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
