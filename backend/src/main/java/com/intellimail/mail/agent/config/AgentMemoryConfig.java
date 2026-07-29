package com.intellimail.mail.agent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Short-term, in-conversation memory for the AI agent - not a persistence
 * guarantee. Backed by an in-process map keyed by conversationId, capped at
 * a message window, and lost on restart; proportionate for "remember the
 * last few turns," not "remember forever" (which was explicitly out of
 * scope for this feature).
 */
@Configuration
public class AgentMemoryConfig {

    private static final int MAX_MESSAGES = 24;

    @Bean
    public ChatMemory agentChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
