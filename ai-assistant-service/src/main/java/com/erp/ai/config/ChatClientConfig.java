package com.erp.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The provider (Claude, OpenAI, Gemini, Ollama) is chosen by spring.ai.model.chat in application.yml.
 * Spring AI builds the ChatModel for that provider, so this code never mentions a provider.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, PromptLoader prompts) {
        return builder
                .defaultSystem(prompts.text("system-finance-assistant"))
                .build();
    }

    /** Keeps the last N messages of each chat in the database (table spring_ai_chat_memory). */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository, AiProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.memory().maxMessages())
                .build();
    }
}
