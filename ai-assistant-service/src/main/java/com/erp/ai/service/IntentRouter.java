package com.erp.ai.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.erp.ai.config.PromptLoader;
import com.erp.ai.dto.Intent;

/** Asks the model to pick one intent. Used to send policy questions to the RAG flow. */
@Service
public class IntentRouter {

    private final ChatClient chatClient;
    private final PromptLoader prompts;

    public IntentRouter(ChatClient chatClient, PromptLoader prompts) {
        this.chatClient = chatClient;
        this.prompts = prompts;
    }

    public Intent classify(String message) {
        String prompt = prompts.render("classify-intent", Map.of("message", message));
        try {
            Intent intent = chatClient.prompt().user(prompt).call().entity(Intent.class);
            return intent == null ? Intent.OTHER : intent;
        } catch (RuntimeException e) {
            return Intent.OTHER; // if the answer cannot be read, use the normal chat flow
        }
    }
}
