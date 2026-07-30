-- Backs the AI agent's conversation memory (see AgentMemoryConfig) with
-- Postgres instead of an in-process map, so it survives backend restarts.
-- Schema copied verbatim from Spring AI 1.0.0's own
-- schema-postgresql.sql (spring-ai-model-chat-memory-repository-jdbc) -
-- JdbcChatMemoryRepository's queries expect exactly this table/column
-- shape, including plain TIMESTAMP (not TIMESTAMPTZ, unlike the rest of
-- this app's schema) and no primary key/id column of our own.
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT        NOT NULL,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"     TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    ON spring_ai_chat_memory (conversation_id, "timestamp");
