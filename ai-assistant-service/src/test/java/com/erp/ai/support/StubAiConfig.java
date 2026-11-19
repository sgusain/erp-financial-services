package com.erp.ai.support;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fake model and embeddings for tests. No network, no API key, same answer every time. */
@TestConfiguration
public class StubAiConfig {

    public static final String STUB_ANSWER = "stub answer";

    @Bean
    @Primary
    ChatModel stubChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(STUB_ANSWER))));
            }
        };
    }

    @Bean
    @Primary
    EmbeddingModel stubEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> result = new java.util.ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    result.add(new Embedding(vector(), i));
                }
                return new EmbeddingResponse(result);
            }

            @Override
            public float[] embed(Document document) {
                return vector();
            }

            @Override
            public int dimensions() {
                return 768;
            }

            private float[] vector() {
                float[] v = new float[768];
                v[0] = 1f;
                return v;
            }
        };
    }
}
