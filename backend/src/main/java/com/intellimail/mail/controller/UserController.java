package com.intellimail.mail.controller;

import com.intellimail.mail.dto.user.UpdateProfileRequest;
import com.intellimail.mail.dto.user.UserProfileResponse;
import com.intellimail.mail.security.UserPrincipal;
import com.intellimail.mail.service.UserService;
import com.intellimail.mail.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Authenticated user profile")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get the current user's profile")
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update the current user's profile")
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(principal.getId(), request));
    }
}
