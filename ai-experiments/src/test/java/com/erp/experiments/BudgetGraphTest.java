package com.erp.experiments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.junit.jupiter.api.Test;

class BudgetGraphTest {

    @Test
    void pausesBeforeSubmitAndContinuesAfterApproval() throws Exception {
        CompiledGraph<BudgetState> app = BudgetGraph.build();
        RunnableConfig config = RunnableConfig.builder().threadId("t-approve").build();

        app.stream(Map.of("fiscalYear", 2026), config).forEach(step -> { });

        // Paused: the draft exists but nothing is submitted yet.
        assertThat(app.getState(config).state().proposal()).contains("Marketing");
        assertThat(app.getState(config).state().result()).isEmpty();

        RunnableConfig approved = app.updateState(config, Map.of("approved", true));
        app.stream(GraphInput.resume(), approved).forEach(step -> { });

        assertThat(app.getState(approved).state().result()).startsWith("SUBMITTED");
    }

    @Test
    void loopStopsAfterTheLimit() throws Exception {
        CompiledGraph<BudgetState> app = BudgetGraph.build();
        RunnableConfig config = RunnableConfig.builder().threadId("t-loop").build();

        app.stream(Map.of("fiscalYear", 2026), config).forEach(step -> { });

        // Margin goes 5, 10 percent: the 10 percent pass is enough, so two drafts at most.
        assertThat(app.getState(config).state().loops()).isBetween(1, BudgetGraph.MAX_LOOPS);
    }

    @Test
    void endsEarlyWhenNothingIsOverspent() throws Exception {
        CompiledGraph<BudgetState> app = BudgetGraph.build();
        RunnableConfig config = RunnableConfig.builder().threadId("t-none").build();

        app.stream(Map.of("fiscalYear", 2030), config).forEach(step -> { });

        assertThat(app.getState(config).state().proposal()).isEmpty();
    }
}
