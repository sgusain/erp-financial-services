package com.erp.experiments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

class LangChain4jAgentTest {

    @Test
    void toolReturnsRealNumbers() {
        String status = new BudgetTools(SampleData.budgets()).getBudgetStatus(2026);

        assertThat(status).contains("Marketing").contains("122.0 percent used");
    }

    @Test
    void agentIsWiredAndAnswersThroughTheModel() {
        ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from("Marketing is over budget.")).build();
            }
        };

        String answer = LangChain4jAgent.build(stub).analyze("Analyze budgets for fiscal year 2026");

        assertThat(answer).contains("Marketing");
    }
}
