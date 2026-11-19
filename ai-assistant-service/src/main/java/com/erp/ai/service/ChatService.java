package com.erp.ai.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.erp.ai.advisor.PiiMaskingAdvisor;
import com.erp.ai.advisor.TokenUsageAdvisor;
import com.erp.ai.config.AiProperties;
import com.erp.ai.dto.ChatRequest;
import com.erp.ai.dto.ChatResponse;
import com.erp.ai.dto.Intent;
import com.erp.ai.dto.PolicyAnswer;
import com.erp.ai.limits.AiGuard;
import com.erp.ai.limits.TokenBudgetService;
import com.erp.ai.tool.ErpReadTools;
import com.erp.ai.tool.ErpWriteTools;
import com.erp.ai.tool.TokenResolver;

import reactor.core.publisher.Flux;

/**
 * The main chat flow:
 * 1. rate limit and daily token check
 * 2. find or create the chat
 * 3. (optional) work out the intent; policy questions go to the RAG flow
 * 4. everything else: ask the model, with memory, PII masking and tools
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final PiiMaskingAdvisor piiMasking;
    private final TokenUsageAdvisor tokenUsage;
    private final ErpReadTools readTools;
    private final ErpWriteTools writeTools;
    private final IntentRouter intentRouter;
    private final PolicyService policyService;
    private final ConversationService conversations;
    private final AiGuard guard;
    private final TokenBudgetService tokenBudget;
    private final LlmGateway llm;
    private final AiProperties properties;

    public ChatService(ChatClient chatClient, ChatMemory chatMemory, PiiMaskingAdvisor piiMasking,
                       TokenUsageAdvisor tokenUsage, ErpReadTools readTools, ErpWriteTools writeTools,
                       IntentRouter intentRouter, PolicyService policyService, ConversationService conversations,
                       AiGuard guard, TokenBudgetService tokenBudget, LlmGateway llm, AiProperties properties) {
        this.chatClient = chatClient;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.piiMasking = piiMasking;
        this.tokenUsage = tokenUsage;
        this.readTools = readTools;
        this.writeTools = writeTools;
        this.intentRouter = intentRouter;
        this.policyService = policyService;
        this.conversations = conversations;
        this.guard = guard;
        this.tokenBudget = tokenBudget;
        this.llm = llm;
        this.properties = properties;
    }

    public ChatResponse chat(String username, String token, ChatRequest request) {
        guard.check(username);
        String chatId = conversations.ensure(username, request.chatId(), request.message());

        Intent intent = properties.intentRoutingEnabled()
                ? llm.call(() -> intentRouter.classify(request.message()))
                : Intent.OTHER;

        if (intent == Intent.POLICY) {
            PolicyAnswer policy = policyService.ask(username, request.message());
            String sources = policy.sources().isEmpty() ? "" : "\n\nSources: " + String.join(", ", policy.sources());
            return new ChatResponse(chatId, policy.answer() + sources, intent);
        }

        String answer = llm.call(() -> prompt(username, token, chatId, request.message()).call().content());
        return new ChatResponse(chatId, answer, intent);
    }

    /**
     * Streams the answer word by word as server-sent events:
     * event "chat" (the chat id), then many "token" events, then "done".
     */
    public Flux<ServerSentEvent<String>> stream(String username, String token, ChatRequest request) {
        guard.check(username);
        String chatId = conversations.ensure(username, request.chatId(), request.message());
        AtomicLong chars = new AtomicLong();

        Flux<ServerSentEvent<String>> tokens = prompt(username, token, chatId, request.message())
                .stream()
                .content()
                .doOnNext(piece -> chars.addAndGet(piece.length()))
                .map(piece -> ServerSentEvent.builder(piece).event("token").build())
                // Streaming does not report usage here, so we estimate: about 4 characters per token.
                .doOnComplete(() -> tokenBudget.add(username, chars.get() / 4))
                .onErrorResume(error -> Flux.just(ServerSentEvent.builder("The AI provider failed. Try again.")
                        .event("error").build()));

        return Flux.concat(
                Flux.just(ServerSentEvent.builder(chatId).event("chat").build()),
                tokens,
                Flux.just(ServerSentEvent.builder("").event("done").build()));
    }

    private ChatClient.ChatClientRequestSpec prompt(String username, String token, String chatId, String message) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .user(message)
                .advisors(piiMasking, memoryAdvisor, tokenUsage)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, ConversationService.memoryKey(username, chatId))
                        .param("username", username))
                .tools(readTools, writeTools)
                // The tools read the user's token from here, so ERP roles apply to every tool call.
                .toolContext(Map.of(TokenResolver.TOKEN_KEY, token, TokenResolver.USERNAME_KEY, username));
        if (properties.logPrompts()) {
            spec = spec.advisors(new SimpleLoggerAdvisor());
        }
        return spec;
    }
}
