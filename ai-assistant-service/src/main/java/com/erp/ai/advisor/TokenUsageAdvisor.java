package com.erp.ai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.erp.ai.limits.TokenBudgetService;

/**
 * Reads how many tokens the model used and adds them to the user's daily total.
 * The caller passes the username as an advisor parameter called "username".
 */
@Component
public class TokenUsageAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    private final TokenBudgetService tokenBudget;

    public TokenUsageAdvisor(TokenBudgetService tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        Object username = request.context().get("username");
        if (username != null && response.chatResponse() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            Integer total = usage == null ? null : usage.getTotalTokens();
            if (total != null) {
                log.info("AI tokens used: user={} total={}", username, total);
                tokenBudget.add(username.toString(), total);
            }
        }
        return response;
    }
}
