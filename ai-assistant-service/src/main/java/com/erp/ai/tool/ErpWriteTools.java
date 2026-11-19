package com.erp.ai.tool;

import java.math.BigDecimal;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.erp.ai.service.PendingActionService;

/**
 * The one write tool. It does NOT change the budget. It only saves a request.
 * A person must confirm it with POST /api/ai/actions/{id}/confirm.
 */
@Component
public class ErpWriteTools {

    private final PendingActionService pendingActions;

    public ErpWriteTools(PendingActionService pendingActions) {
        this.pendingActions = pendingActions;
    }

    @Tool(description = "Propose a new total amount for a budget. This only creates a request that the user must confirm. It does not change the budget.")
    public String requestBudgetChange(
            @ToolParam(description = "Numeric budget id") Long budgetId,
            @ToolParam(description = "The new total amount, greater than zero") BigDecimal newTotalAmount,
            @ToolParam(description = "Short reason for the change") String reason,
            ToolContext context) {
        String id = pendingActions.create(TokenResolver.username(context), budgetId, newTotalAmount, reason);
        return "Request " + id + " created. It is waiting for the user to confirm. The budget is NOT changed yet.";
    }
}
