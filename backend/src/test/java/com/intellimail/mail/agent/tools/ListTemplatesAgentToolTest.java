package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.dto.common.PageResponse;
import com.intellimail.mail.dto.template.PromptTemplateResponse;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.service.PromptTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTemplatesAgentToolTest {

    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private AgentStepRecorder stepRecorder;

    private ListTemplatesAgentTool tool;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        tool = new ListTemplatesAgentTool(promptTemplateService, stepRecorder);
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        AgentExecutionContext.set(userId, organizationId, UUID.randomUUID(), null);
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void listTemplates_filtersByKeyword_caseInsensitively() {
        UUID declineId = UUID.randomUUID();
        PromptTemplateResponse decline = new PromptTemplateResponse(
                declineId, "Decline Meeting", "desc", RequestType.GENERATE_REPLY, "...", null, false, userId, null, null);
        PromptTemplateResponse sales = new PromptTemplateResponse(
                UUID.randomUUID(), "Sales Pitch", "desc", RequestType.SALES, "...", null, false, userId, null, null);
        Page<PromptTemplateResponse> page = new PageImpl<>(List.of(decline, sales));
        when(promptTemplateService.getTemplates(eq(userId), eq(organizationId), any(Pageable.class)))
                .thenReturn(PageResponse.from(page));

        String result = tool.listTemplates("decline");

        assertThat(result).contains(declineId.toString()).contains("Decline Meeting").doesNotContain("Sales Pitch");
        verify(stepRecorder).record(eq("listTemplates"), eq("decline"), any(), eq(AgentStepStatus.SUCCESS));
    }

    @Test
    void listTemplates_withNoKeyword_returnsAllUpToLimit() {
        PromptTemplateResponse t = new PromptTemplateResponse(
                UUID.randomUUID(), "Thank You Note", "desc", RequestType.THANK_YOU, "...", null, false, userId, null, null);
        when(promptTemplateService.getTemplates(eq(userId), eq(organizationId), any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(t))));

        String result = tool.listTemplates(null);

        assertThat(result).contains("Thank You Note");
    }

    @Test
    void listTemplates_withNoMatches_returnsFriendlyMessage() {
        when(promptTemplateService.getTemplates(eq(userId), eq(organizationId), any(PageRequest.class)))
                .thenReturn(PageResponse.from(Page.empty()));

        String result = tool.listTemplates("nonexistent");

        assertThat(result).contains("No saved templates found");
    }
}
