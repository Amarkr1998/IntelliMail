package com.intellimail.mail.dto.organization;

import com.intellimail.mail.enums.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank @Email String email,
        @NotNull OrgRole orgRole
) {
}
