package com.erp.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.erp.ai.tool.client.AccountClient;
import com.erp.ai.tool.client.AccountView;
import com.erp.ai.tool.client.BudgetClient;
import com.erp.ai.tool.client.BudgetView;
import com.erp.ai.tool.client.TransactionClient;
import com.erp.ai.tool.client.TransactionView;

/**
 * The only place that reads ERP data. Chat tools, the agent and the MCP server all use it.
 * Every method gets the user's token and forwards it, so the ERP services decide what the user may see.
 */
@Service
public class ErpDataService {

    private static final int MAX_ROWS = 50;

    private final AccountClient accounts;
    private final TransactionClient transactions;
    private final BudgetClient budgets;

    public ErpDataService(AccountClient accounts, TransactionClient transactions, BudgetClient budgets) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.budgets = budgets;
    }

    /** A budget with the percent used, so the model does not have to do the maths. */
    public record BudgetStatus(Long id, String name, Integer fiscalYear, BigDecimal totalAmount,
                               BigDecimal spentAmount, BigDecimal percentUsed, String status) {}

    public List<AccountView> listAccounts(String token) {
        return accounts.list(token, 0, MAX_ROWS).content();
    }

    public AccountView getAccount(String token, Long id) {
        return accounts.get(token, id);
    }

    public TransactionView getTransaction(String token, Long id) {
        return transactions.get(token, id);
    }

    /** Newest transactions first. accountId can be null for all accounts. */
    public List<TransactionView> recentTransactions(String token, Long accountId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(days, 1));
        return transactions.list(token, 0, 100, "transactionDate,desc").content().stream()
                .filter(t -> accountId == null || accountId.equals(t.accountId()))
                .filter(t -> isAfter(t.transactionDate(), since))
                .limit(MAX_ROWS)
                .toList();
    }

    public List<BudgetStatus> budgetStatus(String token, int fiscalYear) {
        return budgets.list(token, 0, 100).content().stream()
                .filter(b -> b.fiscalYear() != null && b.fiscalYear() == fiscalYear)
                .map(ErpDataService::toStatus)
                .sorted(Comparator.comparing(BudgetStatus::percentUsed).reversed())
                .toList();
    }

    public BudgetView getBudget(String token, Long id) {
        return budgets.get(token, id);
    }

    public BudgetView updateBudgetTotal(String token, Long id, BigDecimal newTotal) {
        BudgetView current = budgets.get(token, id);
        return budgets.update(token, id, current.withTotalAmount(newTotal));
    }

    private static BudgetStatus toStatus(BudgetView b) {
        BigDecimal total = b.totalAmount() == null ? BigDecimal.ZERO : b.totalAmount();
        BigDecimal spent = b.spentAmount() == null ? BigDecimal.ZERO : b.spentAmount();
        BigDecimal percent = total.signum() == 0
                ? BigDecimal.ZERO
                : spent.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        return new BudgetStatus(b.id(), b.budgetName(), b.fiscalYear(), total, spent, percent, b.status());
    }

    private static boolean isAfter(String date, LocalDateTime since) {
        try {
            return date != null && LocalDateTime.parse(date).isAfter(since);
        } catch (RuntimeException e) {
            return true; // if the date cannot be read, keep the row rather than hide it
        }
    }
}
