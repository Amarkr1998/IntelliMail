package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.dto.organization.AcceptInvitationRequest;
import com.intellimail.mail.dto.organization.CreateOrganizationRequest;
import com.intellimail.mail.dto.organization.InviteMemberRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.enums.OrgRole;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.repository.RoleRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: register -> create organization -> invite -> accept -> member
 * list -> remove member, exercising the real Spring Security + JPA/H2 stack.
 * {@link JavaMailSender} is mocked, same as {@code PasswordResetFlowIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationFlowIntegrationTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"&\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void ensureDefaultRoleExists() {
        roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build()));
    }

    private String registerAndGetAccessToken(String fullName, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(fullName, email, "password123");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    @Test
    void createOrganization_invite_accept_thenMemberListAndRemoval() throws Exception {
        String ownerToken = registerAndGetAccessToken("Org Owner", "org.owner@intellimail.com");
        String memberToken = registerAndGetAccessToken("Org Member", "org.member@intellimail.com");

        // Create organization as owner
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("Acme Inc", "acme-flow-test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("acme-flow-test"));

        mockMvc.perform(get("/api/organizations/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme Inc"));

        // Owner invites the member
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mockMvc.perform(post("/api/organizations/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteMemberRequest("org.member@intellimail.com", OrgRole.MEMBER))))
                .andExpect(status().isCreated());

        verify(javaMailSender).send(any(MimeMessage.class));
        String rawToken = extractToken(mimeMessage.getContent().toString());

        // Member accepts
        mockMvc.perform(post("/api/organizations/invitations/accept")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInvitationRequest(rawToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("acme-flow-test"));

        // Member now shows up in the member list
        String membersBody = mockMvc.perform(get("/api/organizations/members").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(membersBody).contains("org.member@intellimail.com").contains("org.owner@intellimail.com");

        // A non-owner/admin member cannot invite others
        mockMvc.perform(post("/api/organizations/invitations")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteMemberRequest("someone@intellimail.com", OrgRole.MEMBER))))
                .andExpect(status().isForbidden());

        // Owner removes the member
        String memberId = extractMemberId(membersBody, "org.member@intellimail.com");
        assertThat(memberId).isNotNull();

        mockMvc.perform(delete("/api/organizations/members/" + memberId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void createOrganization_thenSecondAttempt_isRejectedWithConflict() throws Exception {
        String token = registerAndGetAccessToken("Solo Founder", "solo.founder@intellimail.com");

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("First Org", "first-org-test"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("Second Org", "second-org-test"))))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptInvitation_withWrongAccountEmail_isRejected() throws Exception {
        String ownerToken = registerAndGetAccessToken("Owner Two", "owner.two@intellimail.com");
        String wrongUserToken = registerAndGetAccessToken("Wrong User", "wrong.user@intellimail.com");

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("Second Acme", "second-acme-test"))))
                .andExpect(status().isCreated());

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mockMvc.perform(post("/api/organizations/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteMemberRequest("intended.invitee@intellimail.com", OrgRole.MEMBER))))
                .andExpect(status().isCreated());

        String rawToken = extractToken(mimeMessage.getContent().toString());

        mockMvc.perform(post("/api/organizations/invitations/accept")
                        .header("Authorization", "Bearer " + wrongUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInvitationRequest(rawToken))))
                .andExpect(status().isUnauthorized());
    }

    private String extractToken(String html) {
        Matcher matcher = TOKEN_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("No invitation token found in email body: " + html);
    }

    private String extractMemberId(String membersJson, String email) throws Exception {
        var contentArray = objectMapper.readTree(membersJson).at("/data/content");
        for (var node : contentArray) {
            if (email.equals(node.get("email").asText())) {
                return node.get("id").asText();
            }
        }
        return null;
    }
}
