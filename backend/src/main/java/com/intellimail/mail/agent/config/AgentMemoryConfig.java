package com.intellimail.mail.agent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-conversation memory for the AI agent, keyed by conversationId - the
 * frontend holds that id for as long as "Conversation active" is shown and
 * only mints a fresh one when the user clicks "Start New Conversation", so
 * a new conversationId is what actually clears memory, not eviction.
 *
 * <p>maxMessages is a real ceiling, not a design choice to forget early
 * turns: every message in a conversation is replayed into each subsequent
 * model call, and Azure OpenAI (like any LLM) has a finite context window -
 * an unbounded conversation would eventually fail with a context-length
 * error rather than silently keep working. 200 is generous enough that no
 * realistic single-page-session conversation reaches it while still
 * bounding the worst case.
 *
 * <p>Not a persistence guarantee across backend restarts - it's an
 * in-process map ({@link InMemoryChatMemoryRepository}), acceptable since
 * "the current session" was the ask, not "forever."
 */
@Configuration
public class AgentMemoryConfig {

    private static final int MAX_MESSAGES = 200;

    @Bean
    public ChatMemory agentChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
