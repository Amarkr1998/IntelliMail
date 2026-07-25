package com.intellimail.mail.dto.template;

import com.intellimail.mail.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Powers POST /api/templates and PUT /api/templates/{id}. */
public record PromptTemplateRequest(

        @NotBlank(message = "Template name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Category is required")
        RequestType category,

        @NotBlank(message = "Prompt text is required")
        String promptText,

        String systemPrompt,

        boolean isPublic
) {
}
