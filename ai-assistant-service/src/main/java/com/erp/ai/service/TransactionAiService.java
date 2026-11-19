package com.erp.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.erp.ai.advisor.PiiMaskingAdvisor;
import com.erp.ai.advisor.TokenUsageAdvisor;
import com.erp.ai.config.PromptLoader;
import com.erp.ai.dto.ExplainResponse;
import com.erp.ai.dto.TransactionCategory;
import com.erp.ai.limits.AiGuard;
import com.erp.ai.tool.client.TransactionView;

/**
 * Small one-shot AI features on a single transaction: explain, categorize, tags, extract.
 * They show prompt templates and structured output (the model returns JSON that becomes a Java object).
 */
@Service
public class TransactionAiService {

    private final ChatClient chatClient;
    private final PromptLoader prompts;
    private final ErpDataService erp;
    private final LlmGateway llm;
    private final AiGuard guard;
    private final PiiMaskingAdvisor piiMasking;
    private final TokenUsageAdvisor tokenUsage;

    public TransactionAiService(ChatClient chatClient, PromptLoader prompts, ErpDataService erp, LlmGateway llm,
                                AiGuard guard, PiiMaskingAdvisor piiMasking, TokenUsageAdvisor tokenUsage) {
        this.chatClient = chatClient;
        this.prompts = prompts;
        this.erp = erp;
        this.llm = llm;
        this.guard = guard;
        this.piiMasking = piiMasking;
        this.tokenUsage = tokenUsage;
    }

    public ExplainResponse explain(String username, String token, Long transactionId) {
        guard.check(username);
        TransactionView t = erp.getTransaction(token, transactionId);
        String prompt = prompts.render("explain-transaction", Map.of(
                "accountId", String.valueOf(t.accountId()),
                "type", String.valueOf(t.type()),
                "amount", String.valueOf(t.amount()),
                "description", String.valueOf(t.description()),
                "date", String.valueOf(t.transactionDate())));
        String text = llm.call(() -> ask(username, prompt).call().content());
        return new ExplainResponse(transactionId, text);
    }

    public TransactionCategory categorize(String username, String description, java.math.BigDecimal amount) {
        guard.check(username);
        String prompt = prompts.render("categorize", Map.of("description", description, "amount", amount.toPlainString()));
        return llm.call(() -> ask(username, prompt).call().entity(TransactionCategory.class));
    }

    public List<String> tags(String username, String description, java.math.BigDecimal amount) {
        guard.check(username);
        String prompt = prompts.render("tags", Map.of("description", description, "amount", amount.toPlainString()));
        return llm.call(() -> ask(username, prompt).call().entity(new ParameterizedTypeReference<List<String>>() {}));
    }

    /** Pulls key-value fields out of free text. */
    public Map<String, String> extract(String username, String text) {
        guard.check(username);
        String prompt = prompts.render("summary-fields", Map.of("text", text));
        return llm.call(() -> ask(username, prompt).call().entity(new ParameterizedTypeReference<Map<String, String>>() {}));
    }

    private ChatClient.ChatClientRequestSpec ask(String username, String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .advisors(piiMasking, tokenUsage)
                .advisors(a -> a.param("username", username));
    }
}
