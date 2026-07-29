package com.intellimail.mail.dto.agent;

import java.util.Map;

public record PendingActionResponse(
        String actionType,
        Map<String, Object> payload
) {
}
