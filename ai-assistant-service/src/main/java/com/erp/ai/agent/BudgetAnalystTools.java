package com.erp.ai.agent;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.erp.ai.service.ErpDataService;
import com.erp.ai.service.PendingActionService;

/**
 * The tools for ONE agent run. A new object is created per run, so it can carry the user's token
 * and count the steps. After maxSteps, every tool answers LIMIT_REACHED so the loop cannot run forever.
 */
public class BudgetAnalystTools {

    static final String LIMIT_REACHED = "LIMIT_REACHED: step limit reached. Stop calling tools and give your final answer.";

    private final ErpDataService erp;
    private final PendingActionService pendingActions;
    private final String token;
    private final String username;
    private final int maxSteps;
    private final AtomicInteger steps = new AtomicInteger();

    public BudgetAnalystTools(ErpDataService erp, PendingActionService pendingActions,
                              String token, String username, int maxSteps) {
        this.erp = erp;
        this.pendingActions = pendingActions;
        this.token = token;
        this.username = username;
        this.maxSteps = maxSteps;
    }

    public int stepsUsed() {
        return Math.min(steps.get(), maxSteps);
    }

    public boolean hitLimit() {
        return steps.get() > maxSteps;
    }

    @Tool(description = "Show budgets for a fiscal year with total, spent, percent used and status.")
    public Object getBudgetStatus(@ToolParam(description = "Fiscal year, for example 2026") int fiscalYear) {
        if (limitReached()) {
            return LIMIT_REACHED;
        }
        return erp.budgetStatus(token, fiscalYear);
    }

    @Tool(description = "List recent transactions, newest first. Leave accountId empty for all accounts.")
    public Object listRecentTransactions(
            @ToolParam(description = "Numeric account id, or empty for all accounts", required = false) Long accountId,
            @ToolParam(description = "How many days back to look") int days) {
        if (limitReached()) {
            return LIMIT_REACHED;
        }
        return erp.recentTransactions(token, accountId, days);
    }

    @Tool(description = "Propose a new total amount for a budget. Only creates a request that a human must confirm.")
    public Object requestBudgetChange(
            @ToolParam(description = "Numeric budget id") Long budgetId,
            @ToolParam(description = "The new total amount") BigDecimal newTotalAmount,
            @ToolParam(description = "Short reason") String reason) {
        if (limitReached()) {
            return LIMIT_REACHED;
        }
        String id = pendingActions.create(username, budgetId, newTotalAmount, reason);
        return "Request " + id + " created and waiting for a human to confirm. The budget is NOT changed yet.";
    }

    private boolean limitReached() {
        return steps.incrementAndGet() > maxSteps;
    }
}
