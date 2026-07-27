package com.intellimail.mail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimail.mail.dto.auth.GoogleLoginRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.repository.RoleRepository;
import com.intellimail.mail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises POST /api/auth/google through the real Spring Security + JPA/H2
 * stack. {@link JwtDecoder} is mocked - the only genuinely external call in
 * this flow (Google's JWKS endpoint) - everything downstream is real,
 * following the same mocking boundary already established for
 * {@code JavaMailSender} in the forgot-password/organization-invitation flows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleAuthIntegrationTest {

    private static final String CLIENT_ID = "test-google-client-id.apps.googleusercontent.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder googleJwtDecoder;

    @BeforeEach
    void ensureDefaultRoleExists() {
        roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build()));
    }

    private Jwt validJwt(String subject, String email) {
        return Jwt.withTokenValue("raw-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("aud", CLIENT_ID)
                .claim("iss", "https://accounts.google.com")
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", "Google Test User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void loginWithGoogle_newAccount_registersAndReturnsTokens() throws Exception {
        when(googleJwtDecoder.decode("raw-token")).thenReturn(validJwt("google-sub-int-1", "google.newuser@intellimail.com"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("raw-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("google.newuser@intellimail.com"));

        assertUserExistsWithGoogleSubject("google.newuser@intellimail.com", "google-sub-int-1");
    }

    @Test
    void loginWithGoogle_sameGoogleAccountTwice_reusesTheSameUser() throws Exception {
        when(googleJwtDecoder.decode("raw-token")).thenReturn(validJwt("google-sub-int-2", "google.repeat@intellimail.com"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("raw-token"))))
                .andExpect(status().isOk());

        long countAfterFirst = userRepository.count();

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("raw-token"))))
                .andExpect(status().isOk());

        assertThat(userRepository.count()).isEqualTo(countAfterFirst);
    }

    @Test
    void loginWithGoogle_invalidToken_isRejectedWithUnauthorized() throws Exception {
        when(googleJwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("bad signature"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    private void assertUserExistsWithGoogleSubject(String email, String expectedSubject) {
        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getGoogleSubjectId()).isEqualTo(expectedSubject);
    }
}
