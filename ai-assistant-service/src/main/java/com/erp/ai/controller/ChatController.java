package com.erp.ai.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.dto.ChatRequest;
import com.erp.ai.dto.ChatResponse;
import com.erp.ai.dto.ChatSummary;
import com.erp.ai.dto.UsageView;
import com.erp.ai.limits.TokenBudgetService;
import com.erp.ai.config.AiProperties;
import com.erp.ai.security.CurrentUser;
import com.erp.ai.service.ChatService;
import com.erp.ai.service.ConversationService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversations;
    private final TokenBudgetService tokenBudget;
    private final AiProperties properties;

    public ChatController(ChatService chatService, ConversationService conversations,
                          TokenBudgetService tokenBudget, AiProperties properties) {
        this.chatService = chatService;
        this.conversations = conversations;
        this.tokenBudget = tokenBudget;
        this.properties = properties;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(CurrentUser.username(), CurrentUser.bearerToken(), request));
    }

    /** Server-sent events. Read the user and token here, on the request thread, before streaming starts. */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(CurrentUser.username(), CurrentUser.bearerToken(), request);
    }

    @GetMapping("/chats")
    public ResponseEntity<Page<ChatSummary>> chats(Pageable pageable) {
        return ResponseEntity.ok(conversations.list(CurrentUser.username(), pageable));
    }

    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String chatId) {
        conversations.delete(CurrentUser.username(), chatId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usage")
    public ResponseEntity<UsageView> usage() {
        return ResponseEntity.ok(new UsageView(
                tokenBudget.usedToday(CurrentUser.username()), properties.limits().dailyTokensPerUser()));
    }
}
