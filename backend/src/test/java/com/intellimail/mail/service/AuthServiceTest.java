package com.intellimail.mail.service;

import com.intellimail.mail.config.JwtProperties;
import com.intellimail.mail.dto.auth.AuthResponse;
import com.intellimail.mail.dto.auth.LoginRequest;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.exception.EmailAlreadyExistsException;
import com.intellimail.mail.exception.InvalidCredentialsException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.mapper.UserMapper;
import com.intellimail.mail.mapper.UserMapperImpl;
import com.intellimail.mail.repository.RoleRepository;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.security.JwtService;
import com.intellimail.mail.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogRecorder auditLogRecorder;
    @Mock
    private HttpServletRequest httpServletRequest;

    private final UserMapper userMapper = new UserMapperImpl();
    private final JwtProperties jwtProperties =
            new JwtProperties("secret", 900_000L, 604_800_000L, "intellimail-test");

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, roleRepository, passwordEncoder, jwtService, jwtProperties, userMapper, auditLogRecorder);
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        when(userRepository.existsByEmail("taken@intellimail.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Name", "taken@intellimail.com", "password123");

        assertThatThrownBy(() -> authService.register(request, httpServletRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(roleRepository);
    }

    @Test
    void register_savesUserWithEncodedPasswordAndDefaultRole() {
        when(userRepository.existsByEmail("new@intellimail.com")).thenReturn(false);
        Role userRole = Role.builder().name(RoleName.ROLE_USER).build();
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh-token");

        RegisterRequest request = new RegisterRequest("New User", "new@intellimail.com", "password123");

        AuthResponse response = authService.register(request, httpServletRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("new@intellimail.com");
        assertThat(response.user().roles()).containsExactly("ROLE_USER");
        verify(passwordEncoder).encode("password123");
        verify(auditLogRecorder).record(any(User.class), eq("USER_REGISTERED"), eq("User"), any(), any(), eq(httpServletRequest));
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        User user = User.builder()
                .fullName("User")
                .email("user@intellimail.com")
                .password("encoded")
                .build();
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("user@intellimail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest("user@intellimail.com", "wrong");

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(auditLogRecorder).record(isNull(), eq("LOGIN_FAILED"), eq("User"), any(), any(), eq(httpServletRequest));
    }
}
