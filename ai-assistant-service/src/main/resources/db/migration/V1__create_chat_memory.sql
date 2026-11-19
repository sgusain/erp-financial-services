-- Table used by Spring AI JdbcChatMemoryRepository. Column names must match what the
-- Spring AI version expects (check the JDBC chat memory schema for the version you use).
-- conversation_id is wider than the default 36 because we use "<username>:<chatId>".
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(10)  NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"     TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conversation
    ON spring_ai_chat_memory (conversation_id, "timestamp");

-- One row per chat, so users can list and delete their chats.
CREATE TABLE IF NOT EXISTS chat_conversation (
    id         UUID PRIMARY KEY,
    username   VARCHAR(100) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_conversation_username
    ON chat_conversation (username, created_at DESC);
