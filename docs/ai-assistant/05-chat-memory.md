# 05. Chat memory (Course 1 parts 6, 7)

## Goal
Multi-turn conversations stored per user, with a bounded window.

## Steps
1. Flyway `V1__create_chat_memory.sql` for the Spring AI JDBC chat memory table (use the schema the chosen Spring AI version expects; check the docs for that version). Add `chat_conversation` (id, user_id, title, created_at) for listing chats.
2. Configure JDBC chat memory with a message window (default last 20 messages, property `erp.ai.memory.max-messages`).
3. Conversation id = `{userId}:{chatId}`. `userId` comes from the JWT. The client only sends `chatId`.
4. Endpoints: `POST /api/ai/chat` accepts optional `chatId` (creates one if missing). `GET /api/ai/chats` (Pageable). `GET /api/ai/chats/{chatId}/messages`. `DELETE /api/ai/chats/{chatId}`.
5. Ownership check: a user asking for another user's chat gets 404 (not 403, do not leak existence).
6. Add the memory advisor to the ChatClient after the PII masking advisor. Confirm stored messages are the masked ones.

## Done when
- "What is my balance?" then "and last month?" keeps context.
- Integration test (Testcontainers Postgres): user A cannot read or delete user B's chat.
- Window limit is respected (old messages are not sent).

## Interview notes
- "The model has no memory. I store messages in the database and send the last few with each call. I key them by user id from the token so users never see each other's chats. A bigger window costs more tokens."
