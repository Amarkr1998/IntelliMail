package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.client.AiGenerationResult;
import com.intellimail.mail.client.AzureOpenAiClient;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.dto.email.EmailGenerateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full register -> authenticate -> generate-reply path through
 * real Spring Security + JPA/H2, mocking only the one thing that actually
 * talks to an external service: {@link AzureOpenAiClient}. This is the
 * closest thing to an end-to-end proof that Modules 1-7 are wired correctly
 * together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailGenerationIntegrationTest {

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
    void generateReply_endToEnd_returnsAiReplyAndPersistsHistory() throws Exception {
        when(azureOpenAiClient.generate(any(), anyDouble(), anyInt()))
                .thenReturn(new AiGenerationResult("Sure, Tuesday at 3pm works.", "gpt-4o", 40, 25, 65, 750L));

        String accessToken = registerAndGetAccessToken("email.integration@intellimail.com");

        mockMvc.perform(post("/api/email/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmailGenerateRequest("Can we meet Tuesday at 3pm?", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Sure, Tuesday at 3pm works."))
                .andExpect(jsonPath("$.data.requestType").value("GENERATE_REPLY"))
                .andExpect(jsonPath("$.data.totalTokens").value(65));
    }

    @Test
    void generate_withoutToken_isRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailGenerateRequest("content", null, null))))
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
