package com.intellimail.mail.service;

import com.intellimail.mail.config.JwtProperties;
import com.intellimail.mail.dto.auth.AuthResponse;
import com.intellimail.mail.dto.auth.LoginRequest;
import com.intellimail.mail.dto.auth.RefreshTokenRequest;
import com.intellimail.mail.dto.auth.RegisterRequest;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import com.intellimail.mail.exception.EmailAlreadyExistsException;
import com.intellimail.mail.exception.InvalidCredentialsException;
import com.intellimail.mail.exception.InvalidTokenException;
import com.intellimail.mail.logging.AuditLogRecorder;
import com.intellimail.mail.mapper.UserMapper;
import com.intellimail.mail.repository.RoleRepository;
import com.intellimail.mail.repository.UserRepository;
import com.intellimail.mail.security.JwtService;
import com.intellimail.mail.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final AuditLogRecorder auditLogRecorder;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role ROLE_USER is not seeded — check Flyway migration V9__seed_roles.sql"));

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(defaultRole))
                .build();
        user = userRepository.save(user);

        auditLogRecorder.record(user, "USER_REGISTERED", "User", user.getId().toString(), null, httpRequest);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            auditLogRecorder.record(null, "LOGIN_FAILED", "User", user.getId().toString(), "Bad password", httpRequest);
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        auditLogRecorder.record(user, "LOGIN_SUCCESS", "User", user.getId().toString(), null, httpRequest);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtService.validateToken(request.refreshToken(), JwtService.TOKEN_TYPE_REFRESH);
        UUID userId = jwtService.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User for refresh token no longer exists"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.of(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return AuthResponse.of(accessToken, refreshToken, jwtProperties.accessTokenExpirationMs(), userMapper.toProfileResponse(user));
    }
}
