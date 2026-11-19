package com.erp.ai.tool;

import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.erp.ai.service.ErpDataService;
import com.erp.ai.service.ErpDataService.BudgetStatus;
import com.erp.ai.tool.client.AccountView;
import com.erp.ai.tool.client.TransactionView;

/**
 * Read-only tools. The model can call these on its own to get real data.
 * The description text is what the model reads to decide when to use each tool.
 * These are also the tools we show over MCP (read-only on purpose).
 */
@Component
public class ErpReadTools {

    private final ErpDataService erp;

    public ErpReadTools(ErpDataService erp) {
        this.erp = erp;
    }

    @Tool(description = "List ERP accounts with code, name, type and current balance.")
    public List<AccountView> listAccounts(ToolContext context) {
        return erp.listAccounts(TokenResolver.token(context));
    }

    @Tool(description = "Get one ERP account by its numeric id, including the current balance.")
    public AccountView getAccount(@ToolParam(description = "Numeric account id") Long accountId,
                                  ToolContext context) {
        return erp.getAccount(TokenResolver.token(context), accountId);
    }

    @Tool(description = "List recent transactions, newest first. Leave accountId empty for all accounts.")
    public List<TransactionView> listRecentTransactions(
            @ToolParam(description = "Numeric account id, or empty for all accounts", required = false) Long accountId,
            @ToolParam(description = "How many days back to look, for example 30") int days,
            ToolContext context) {
        return erp.recentTransactions(TokenResolver.token(context), accountId, days);
    }

    @Tool(description = "Show budgets for a fiscal year with total, spent, percent used and status (ACTIVE, INACTIVE, EXCEEDED).")
    public List<BudgetStatus> getBudgetStatus(
            @ToolParam(description = "Fiscal year, for example 2026") int fiscalYear,
            ToolContext context) {
        return erp.budgetStatus(TokenResolver.token(context), fiscalYear);
    }
}
