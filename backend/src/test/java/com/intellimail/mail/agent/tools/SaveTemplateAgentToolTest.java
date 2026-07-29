package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentPendingActionHolder;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.PendingActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveTemplateAgentToolTest {

    @Mock
    private AgentPendingActionHolder pendingActionHolder;
    @Mock
    private AgentStepRecorder stepRecorder;

    private SaveTemplateAgentTool tool;

    @BeforeEach
    void setUp() {
        tool = new SaveTemplateAgentTool(pendingActionHolder, stepRecorder);
    }

    @Test
    void proposeSaveTemplate_recordsPendingAction_withoutActuallySavingAnything() {
        String result = tool.proposeSaveTemplate("Decline Meeting", "Politely declines", "GENERATE_REPLY",
                "Please write a polite decline", null);

        assertThat(result).contains("Proposed saving this as a template");
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pendingActionHolder).propose(eq(PendingActionType.SAVE_TEMPLATE), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("name")).isEqualTo("Decline Meeting");
        assertThat(payload.get("category")).isEqualTo("GENERATE_REPLY");
        assertThat(payload.get("promptText")).isEqualTo("Please write a polite decline");
        assertThat(payload.get("isPublic")).isEqualTo(false);
        verify(stepRecorder).record(eq("proposeSaveTemplate"), eq("Decline Meeting"), any(), eq(AgentStepStatus.SUCCESS));
    }
}
