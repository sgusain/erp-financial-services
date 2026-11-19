package com.erp.experiments;

import java.util.List;

/** The business rule lives in plain Java. The frameworks only decide WHEN to call it. */
public final class BudgetRules {

    private BudgetRules() {}

    /** Budgets of that year where spending is above the limit. */
    public static List<Budget> overspent(List<Budget> budgets, int fiscalYear) {
        return budgets.stream()
                .filter(b -> b.fiscalYear() == fiscalYear)
                .filter(b -> b.spentAmount() > b.totalAmount())
                .toList();
    }

    /** New limit = spent amount plus a safety margin (0.10 means 10 percent), rounded to whole money. */
    public static double proposedTotal(Budget budget, double margin) {
        return Math.ceil(budget.spentAmount() * (1 + margin));
    }
}
