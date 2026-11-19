package com.erp.ai.limits;

import org.springframework.stereotype.Component;

/** One call that applies both protections before we spend money on the model. */
@Component
public class AiGuard {

    private final RateLimiter rateLimiter;
    private final TokenBudgetService tokenBudget;

    public AiGuard(RateLimiter rateLimiter, TokenBudgetService tokenBudget) {
        this.rateLimiter = rateLimiter;
        this.tokenBudget = tokenBudget;
    }

    public void check(String username) {
        rateLimiter.check(username);
        tokenBudget.check(username);
    }
}
