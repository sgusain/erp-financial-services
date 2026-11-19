package com.erp.experiments;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/** LangChain4j tools: same idea as @Tool in Spring AI, different annotation package. */
public class BudgetTools {

    private final List<Budget> data;

    public BudgetTools(List<Budget> data) {
        this.data = data;
    }

    @Tool("Show budgets of a fiscal year with total, spent and percent used")
    public String getBudgetStatus(@P("Fiscal year, for example 2026") int fiscalYear) {
        return data.stream()
                .filter(b -> b.fiscalYear() == fiscalYear)
                .map(b -> b.name() + " (id " + b.id() + "): total " + b.totalAmount()
                        + ", spent " + b.spentAmount() + ", " + b.percentUsed() + " percent used")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No budgets for " + fiscalYear);
    }
}
