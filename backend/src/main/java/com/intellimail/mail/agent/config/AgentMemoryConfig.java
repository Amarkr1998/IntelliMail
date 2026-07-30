package com.intellimail.mail.agent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-conversation memory for the AI agent, keyed by conversationId - the
 * frontend holds that id for as long as "Conversation active" is shown and
 * only mints a fresh one when the user clicks "Start New Conversation", so
 * a new conversationId is what actually clears memory, not eviction.
 *
 * <p>{@link ChatMemoryRepository} is injected rather than constructed here -
 * {@code spring-ai-starter-model-chat-memory-repository-jdbc} on the
 * classpath auto-configures a {@code JdbcChatMemoryRepository} bean backed
 * by the app's own Postgres {@code DataSource} (table owned by Flyway
 * migration V21, not this starter's own schema initializer - see
 * application.yml's {@code spring.ai.chat.memory.repository.jdbc.initialize-schema: never}).
 * This means conversation memory now survives backend restarts, unlike the
 * in-process map this originally shipped with.
 *
 * <p>maxMessages is still a real ceiling, not a design choice to forget
 * early turns: every message in a conversation is replayed into each
 * subsequent model call, and Azure OpenAI (like any LLM) has a finite
 * context window - an unbounded conversation would eventually fail with a
 * context-length error rather than silently keep working. 200 is generous
 * enough that no realistic single conversation reaches it while still
 * bounding the worst case.
 */
@Configuration
public class AgentMemoryConfig {

    private static final int MAX_MESSAGES = 200;

    @Bean
    public ChatMemory agentChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
