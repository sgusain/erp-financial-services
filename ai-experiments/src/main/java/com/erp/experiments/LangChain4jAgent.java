package com.erp.experiments;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/** Builds the LangChain4j version of the agent. Run main() with ANTHROPIC_API_KEY set to try it. */
public final class LangChain4jAgent {

    private LangChain4jAgent() {}

    public static BudgetAnalyst build(ChatModel model) {
        return AiServices.builder(BudgetAnalyst.class)
                .chatModel(model)
                .tools(new BudgetTools(SampleData.budgets()))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    public static void main(String[] args) {
        ChatModel model = AnthropicChatModel.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName("claude-sonnet-5-5")
                .build();
        System.out.println(build(model).analyze("Analyze budgets for fiscal year 2026"));
    }
}
