package com.erp.ai.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.erp.ai.advisor.TokenUsageAdvisor;
import com.erp.ai.config.AiProperties;
import com.erp.ai.config.PromptLoader;
import com.erp.ai.dto.PolicyAnswer;
import com.erp.ai.limits.AiGuard;

/**
 * RAG over the policy files in src/main/resources/policies:
 * ingest = read files, split into chunks, embed, store in pgvector.
 * ask    = embed the question, find the closest chunks, answer using only those chunks.
 */
@Service
public class PolicyService {

    static final String TYPE = "policy";
    static final String NO_ANSWER = "I don't know. The policy documents do not cover this.";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final PromptLoader prompts;
    private final AiProperties properties;
    private final LlmGateway llm;
    private final AiGuard guard;
    private final TokenUsageAdvisor tokenUsage;

    public PolicyService(VectorStore vectorStore, ChatClient chatClient, PromptLoader prompts,
                         AiProperties properties, LlmGateway llm, AiGuard guard, TokenUsageAdvisor tokenUsage) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.prompts = prompts;
        this.properties = properties;
        this.llm = llm;
        this.guard = guard;
        this.tokenUsage = tokenUsage;
    }

    /** Replaces the policy chunks in the vector store with the current policy files. Returns the number of chunks. */
    public int ingest() {
        List<Document> documents = new ArrayList<>();
        try {
            for (Resource file : new PathMatchingResourcePatternResolver().getResources("classpath:policies/*.md")) {
                String text = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                documents.add(new Document(text, Map.of("source", file.getFilename(), "type", TYPE)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read policy files", e);
        }
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(properties.rag().chunkSize())
                .build();
        List<Document> chunks = splitter.apply(documents);

        vectorStore.delete("type == '" + TYPE + "'"); // only our policy chunks, safe to run again
        vectorStore.add(chunks);
        return chunks.size();
    }

    public PolicyAnswer ask(String username, String question) {
        guard.check(username);
        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(properties.rag().topK())
                .similarityThreshold(properties.rag().similarityThreshold())
                .filterExpression("type == '" + TYPE + "'")
                .build());

        // Nothing close enough found: say so, do not let the model guess.
        if (matches == null || matches.isEmpty()) {
            return new PolicyAnswer(NO_ANSWER, List.of());
        }

        String context = matches.stream()
                .map(d -> "[" + d.getMetadata().get("source") + "]\n" + d.getText())
                .collect(Collectors.joining("\n\n---\n\n"));
        String prompt = prompts.render("policy-answer", Map.of("context", context, "question", question));

        String answer = llm.call(() -> chatClient.prompt()
                .user(prompt)
                .advisors(tokenUsage)
                .advisors(a -> a.param("username", username))
                .call()
                .content());

        List<String> sources = matches.stream()
                .map(d -> String.valueOf(d.getMetadata().get("source")))
                .distinct()
                .toList();
        return new PolicyAnswer(answer, sources);
    }
}
