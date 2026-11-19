package com.erp.ai.service;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erp.ai.dto.ChatSummary;
import com.erp.ai.exception.NotFoundException;

/**
 * One row per chat, so a user can list and delete their chats.
 * The memory itself is stored by Spring AI under the key "<username>:<chatId>".
 * Because the username is part of the key, one user can never read another user's chat.
 */
@Service
public class ConversationService {

    private final JdbcClient jdbc;
    private final ChatMemory chatMemory;

    public ConversationService(JdbcClient jdbc, ChatMemory chatMemory) {
        this.jdbc = jdbc;
        this.chatMemory = chatMemory;
    }

    /** Returns the chat id to use: the given one (if it is yours) or a new one. */
    public String ensure(String username, String chatId, String firstMessage) {
        if (chatId == null || chatId.isBlank()) {
            String id = UUID.randomUUID().toString();
            jdbc.sql("INSERT INTO chat_conversation (id, username, title) VALUES (CAST(:id AS uuid), :username, :title)")
                    .param("id", id)
                    .param("username", username)
                    .param("title", title(firstMessage))
                    .update();
            return id;
        }
        UUID.fromString(chatId); // throws IllegalArgumentException -> 400 if it is not a UUID
        boolean mine = jdbc.sql("SELECT count(*) FROM chat_conversation WHERE id = CAST(:id AS uuid) AND username = :username")
                .param("id", chatId).param("username", username)
                .query(Long.class).single() > 0;
        if (!mine) {
            throw new NotFoundException("Chat not found");
        }
        return chatId;
    }

    public static String memoryKey(String username, String chatId) {
        return username + ":" + chatId;
    }

    public Page<ChatSummary> list(String username, Pageable pageable) {
        List<ChatSummary> rows = jdbc.sql("""
                SELECT id, title, created_at FROM chat_conversation WHERE username = :username
                ORDER BY created_at DESC LIMIT :limit OFFSET :offset
                """)
                .param("username", username)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, n) -> new ChatSummary(rs.getString("id"), rs.getString("title"),
                        rs.getTimestamp("created_at").toLocalDateTime()))
                .list();
        long total = jdbc.sql("SELECT count(*) FROM chat_conversation WHERE username = :username")
                .param("username", username).query(Long.class).single();
        return new PageImpl<>(rows, pageable, total);
    }

    public void delete(String username, String chatId) {
        int rows = jdbc.sql("DELETE FROM chat_conversation WHERE id = CAST(:id AS uuid) AND username = :username")
                .param("id", chatId).param("username", username).update();
        if (rows == 0) {
            throw new NotFoundException("Chat not found");
        }
        chatMemory.clear(memoryKey(username, chatId));
    }

    private static String title(String message) {
        String clean = message.strip().replaceAll("\\s+", " ");
        return clean.length() <= 60 ? clean : clean.substring(0, 60) + "...";
    }
}
