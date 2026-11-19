package com.erp.experiments;

import java.util.Map;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;

/** Run the graph, see it pause, "approve", and see it finish. No API key needed. */
public final class BudgetGraphDemo {

    private BudgetGraphDemo() {}

    public static void main(String[] args) throws Exception {
        CompiledGraph<BudgetState> app = BudgetGraph.build();
        RunnableConfig config = RunnableConfig.builder().threadId("demo-1").build();

        // Run until the pause before "submit".
        app.stream(Map.of("fiscalYear", 2026), config).forEach(step -> System.out.println("step: " + step.node()));

        // A human says yes: write that into the saved state, then continue.
        RunnableConfig approved = app.updateState(config, Map.of("approved", true));
        app.stream(GraphInput.resume(), approved).forEach(step -> System.out.println("step: " + step.node()));

        System.out.println(app.getState(approved).state().result());
    }
}
