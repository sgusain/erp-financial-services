package com.erp.experiments;

import java.util.List;

/** Fixed sample data, so both versions can be compared on exactly the same input. */
public final class SampleData {

    private SampleData() {}

    public static List<Budget> budgets() {
        return List.of(
                new Budget(1, "Marketing", 2026, 50_000, 61_000),
                new Budget(2, "IT Infrastructure", 2026, 120_000, 80_000),
                new Budget(3, "Travel", 2026, 20_000, 19_500),
                new Budget(4, "Old Project", 2025, 10_000, 12_000));
    }
}
