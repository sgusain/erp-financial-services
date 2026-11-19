package com.erp.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** All settings under "erp.ai" in application.yml, in one typed place. */
@ConfigurationProperties(prefix = "erp.ai")
public record AiProperties(
        int embeddingDimensions,
        boolean devLoginEnabled,
        boolean intentRoutingEnabled,
        boolean logPrompts,
        Memory memory,
        Agent agent,
        Rag rag,
        Limits limits) {

    public record Memory(int maxMessages) {}

    public record Agent(int maxSteps, int timeoutSeconds) {}

    public record Rag(int topK, double similarityThreshold, int chunkSize) {}

    public record Limits(int maxMessageLength, int requestsPerMinute,
                         long dailyTokensPerUser, long dailyTokensGlobal) {}
}
