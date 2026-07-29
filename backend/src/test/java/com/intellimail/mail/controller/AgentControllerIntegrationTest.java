package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.dto.agent.AgentTaskRequest;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.AgentTaskStatus;
import com.intellimail.mail.enums.PendingActionType;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.repository.AgentTaskRepository;
import com.intellimail.mail.repository.PromptTemplateRepository;
import com.intellimail.mail.repository.RoleRepository;
import com.intellimail.mail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real agent ChatClient/tool-calling/memory wiring against real
 * Spring Security + JPA/H2, mocking only {@link ChatModel} - the one thing
 * that actually talks to Azure OpenAI. Both the plain "chatClient" bean and
 * the "agentChatClient" bean are built from this same ChatModel, so one mock
 * covers both call layers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentTaskRepository agentTaskRepository;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @MockitoBean
    private ChatModel chatModel;

    @BeforeEach
    void ensureDefaultRoleExists() {
        roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build()));
    }

    @Test
    void runTask_endToEnd_returnsCompletedResult_andPersistsTaskWithNoPendingAction() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(plainTextResponse("Sure, Tuesday works for me."));

        String accessToken = registerAndGetAccessToken("agent.integration@intellimail.com");

        mockMvc.perform(post("/api/agent/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentTaskRequest("Reply politely to this email", "Can we meet Tuesday at 3pm?", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.finalResult").value("Sure, Tuesday works for me."))
                .andExpect(jsonPath("$.data.pendingAction").doesNotExist());
    }

    @Test
    void runTask_withoutToken_isRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/agent/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentTaskRequest("goal", null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_appliesProposal_andCreatesTemplate() throws Exception {
        String accessToken = registerAndGetAccessToken("agent.confirm@intellimail.com");
        User user = userRepository.findByEmail("agent.confirm@intellimail.com").orElseThrow();

        AgentTask task = agentTaskRepository.save(pendingTask(user));

        mockMvc.perform(post("/api/agent/tasks/" + task.getId() + "/confirm")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.pendingAction").doesNotExist());

        assertThat(promptTemplateRepository.findAll())
                .anyMatch(t -> "Decline Meeting".equals(t.getName()) && user.getId().equals(t.getOwner().getId()));
    }

    @Test
    void reject_discardsProposal_withoutCreatingTemplate() throws Exception {
        String accessToken = registerAndGetAccessToken("agent.reject@intellimail.com");
        User user = userRepository.findByEmail("agent.reject@intellimail.com").orElseThrow();

        AgentTask task = agentTaskRepository.save(pendingTask(user));

        mockMvc.perform(post("/api/agent/tasks/" + task.getId() + "/reject")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertThat(promptTemplateRepository.findAll())
                .noneMatch(t -> "Decline Meeting".equals(t.getName()) && user.getId().equals(t.getOwner().getId()));
    }

    private AgentTask pendingTask(User user) {
        return AgentTask.builder()
                .user(user)
                .status(AgentTaskStatus.AWAITING_CONFIRMATION)
                .goal("Save this as a template")
                .finalResult("Proposed saving this as a template titled 'Decline Meeting'.")
                .conversationId(UUID.randomUUID())
                .pendingActionType(PendingActionType.SAVE_TEMPLATE)
                .pendingActionPayload("{\"name\":\"Decline Meeting\",\"description\":\"\",\"category\":\"GENERATE_REPLY\","
                        + "\"promptText\":\"Please write a polite decline\",\"systemPrompt\":\"\",\"isPublic\":false}")
                .build();
    }

    private ChatResponse plainTextResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
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
