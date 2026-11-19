package com.erp.experiments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BudgetRulesTest {

    @Test
    void findsTheOverspentBudgetOfThatYearOnly() {
        var names = BudgetRules.overspent(SampleData.budgets(), 2026).stream().map(Budget::name).toList();

        // "Old Project" is also overspent, but it is fiscal year 2025.
        assertThat(names).containsExactly("Marketing");
    }

    @Test
    void proposedTotalIncludesTheMargin() {
        Budget marketing = SampleData.budgets().get(0); // spent 61000

        assertThat(BudgetRules.proposedTotal(marketing, 0.10)).isEqualTo(67_100.0);
    }
}
