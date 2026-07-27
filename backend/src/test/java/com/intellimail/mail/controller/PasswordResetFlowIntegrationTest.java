package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.dto.auth.LoginRequest;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.entity.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full register -> forgot-password -> reset-password -> login
 * path through the real Spring Security + JPA/H2 stack. {@link JavaMailSender}
 * is mocked - without it, this test would attempt a real outbound SMTP
 * connection in CI with no network path or credentials.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowIntegrationTest {

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

    @Test
    void forgotPassword_thenResetPassword_thenLoginWithNewPassword() throws Exception {
        String email = "reset.integration@intellimail.com";
        RegisterRequest registerRequest = new RegisterRequest("Grace Hopper", email, "oldPassword123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(javaMailSender).send(any(MimeMessage.class));
        String resetLink = extractResetLink(mimeMessage);
        String rawToken = extractToken(resetLink);
        assertThat(rawToken).isNotBlank();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.intellimail.mail.dto.auth.ResetPasswordRequest(rawToken, "newPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "oldPassword123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "newPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void resetPassword_withInvalidToken_isRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.intellimail.mail.dto.auth.ResetPasswordRequest("not-a-real-token", "newPassword456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_withUnknownEmail_stillReturnsOk_andSendsNoMail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-here@intellimail.com\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verifyNoInteractions(javaMailSender);
    }

    private String extractResetLink(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        return content.toString();
    }

    private String extractToken(String html) {
        Matcher matcher = TOKEN_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("No reset token found in email body: " + html);
    }
}
