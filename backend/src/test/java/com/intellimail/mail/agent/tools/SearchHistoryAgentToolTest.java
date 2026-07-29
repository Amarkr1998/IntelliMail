package com.intellimail.mail.agent.tools;

import com.intellimail.mail.agent.AgentExecutionContext;
import com.intellimail.mail.agent.AgentStepRecorder;
import com.intellimail.mail.entity.EmailRequest;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.AgentStepStatus;
import com.intellimail.mail.enums.RequestType;
import com.intellimail.mail.repository.EmailRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchHistoryAgentToolTest {

    @Mock
    private EmailRequestRepository emailRequestRepository;
    @Mock
    private AgentStepRecorder stepRecorder;

    private SearchHistoryAgentTool tool;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tool = new SearchHistoryAgentTool(emailRequestRepository, stepRecorder);
        userId = UUID.randomUUID();
        AgentExecutionContext.set(userId, null, UUID.randomUUID(), null);
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
    }

    @Test
    void searchHistory_returnsSnippetsOfMatches_notFullContent() {
        User user = User.builder().fullName("User").email("user@intellimail.com").password("hashed").build();
        EmailRequest match = EmailRequest.builder()
                .user(user)
                .requestType(RequestType.GENERATE_REPLY)
                .originalContent("Hi Sarah, thanks for reaching out about the project timeline.")
                .build();
        match.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));

        when(emailRequestRepository.findByUserIdAndOriginalContentContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(userId), eq("Sarah"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        String result = tool.searchHistory("Sarah");

        assertThat(result).contains("2026-01-15").contains("Sarah");
        verify(stepRecorder).record(eq("searchHistory"), eq("Sarah"), any(), eq(AgentStepStatus.SUCCESS));
    }

    @Test
    void searchHistory_withNoMatches_returnsFriendlyMessage() {
        when(emailRequestRepository.findByUserIdAndOriginalContentContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(userId), eq("nonexistent"), any(PageRequest.class)))
                .thenReturn(Page.empty());

        String result = tool.searchHistory("nonexistent");

        assertThat(result).contains("No past emails found");
    }
}
