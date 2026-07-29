package com.intellimail.mail.agent.config;

import com.intellimail.mail.agent.tools.ComposeFromScratchAgentTool;
import com.intellimail.mail.agent.tools.FollowupEmailAgentTool;
import com.intellimail.mail.agent.tools.GenerateReplyAgentTool;
import com.intellimail.mail.agent.tools.ImproveEmailAgentTool;
import com.intellimail.mail.agent.tools.ListTemplatesAgentTool;
import com.intellimail.mail.agent.tools.SaveTemplateAgentTool;
import com.intellimail.mail.agent.tools.SearchHistoryAgentTool;
import com.intellimail.mail.agent.tools.SubjectLineAgentTool;
import com.intellimail.mail.agent.tools.SummarizeEmailAgentTool;
import com.intellimail.mail.agent.tools.TranslateEmailAgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A second, separate {@link ChatClient} bean dedicated to the AI agent -
 * deliberately kept distinct from the plain {@code chatClient} bean in
 * {@code AiChatClientConfig} (used by {@code AzureOpenAiClient} for every
 * per-action /api/email/* call), since the agent's own orchestration system
 * prompt and tool loop must never merge with each action's own per-request
 * system prompt built by {@code PromptFactory}.
 */
@Configuration
public class AgentChatClientConfig {

    @Bean
    public ChatClient agentChatClient(
            ChatModel chatModel,
            ChatMemory agentChatMemory,
            GenerateReplyAgentTool generateReplyAgentTool,
            ImproveEmailAgentTool improveEmailAgentTool,
            TranslateEmailAgentTool translateEmailAgentTool,
            SummarizeEmailAgentTool summarizeEmailAgentTool,
            SubjectLineAgentTool subjectLineAgentTool,
            FollowupEmailAgentTool followupEmailAgentTool,
            SaveTemplateAgentTool saveTemplateAgentTool,
            SearchHistoryAgentTool searchHistoryAgentTool,
            ComposeFromScratchAgentTool composeFromScratchAgentTool,
            ListTemplatesAgentTool listTemplatesAgentTool) {
        return ChatClient.builder(chatModel)
                .defaultTools(
                        generateReplyAgentTool,
                        improveEmailAgentTool,
                        translateEmailAgentTool,
                        summarizeEmailAgentTool,
                        subjectLineAgentTool,
                        followupEmailAgentTool,
                        saveTemplateAgentTool,
                        searchHistoryAgentTool,
                        composeFromScratchAgentTool,
                        listTemplatesAgentTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentChatMemory).build())
                .build();
    }
}
