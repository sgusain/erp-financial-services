package com.erp.experiments;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;

/**
 * LangGraph4j style: the flow is a graph. Nodes do work, edges decide what comes next,
 * and the state is saved at each step, so the run can PAUSE and continue later.
 *
 *  fetchData -> analyze -+-> (no overspend) -> END
 *                        +-> draftChange <-+ (loop while the margin is too small, max 3 times)
 *                                 |
 *                                 v
 *                       [pause: human approval]
 *                                 |
 *                               submit -> END
 */
public final class BudgetGraph {

    static final int MAX_LOOPS = 3;
    static final double REQUIRED_MARGIN = 0.10;

    private BudgetGraph() {}

    public static CompiledGraph<BudgetState> build() throws GraphStateException {
        StateGraph<BudgetState> graph = new StateGraph<>(BudgetState.SCHEMA, BudgetState::new);

        // 1. In the real service this would call the ERP. Here it is fixed sample data.
        graph.addNode("fetchData", node_async(state -> Map.of("dataLoaded", true)));

        // 2. The rule is plain Java (BudgetRules). No model is needed for this step.
        graph.addNode("analyze", node_async(state -> {
            List<String> names = BudgetRules.overspent(SampleData.budgets(), state.fiscalYear()).stream()
                    .map(Budget::name).toList();
            return Map.of("overspent", names);
        }));

        // 3. Draft a change. The margin grows each loop until it is big enough.
        graph.addNode("draftChange", node_async(state -> {
            int loops = state.loops() + 1;
            double margin = 0.05 * loops; // 5 percent, then 10, then 15
            Budget first = BudgetRules.overspent(SampleData.budgets(), state.fiscalYear()).get(0);
            String proposal = first.name() + ": raise total to " + BudgetRules.proposedTotal(first, margin)
                    + " (margin " + Math.round(margin * 100) + " percent)";
            return Map.of("loops", loops, "margin", margin, "proposal", proposal);
        }));

        // 4. The graph stops BEFORE this node (see interruptBefore). It only runs after approval.
        graph.addNode("submit", node_async(state ->
                Map.of("result", state.approved() ? "SUBMITTED: " + state.proposal() : "REJECTED")));

        graph.addEdge(START, "fetchData");
        graph.addEdge("fetchData", "analyze");

        // Early end: nothing is overspent, so there is nothing to do.
        graph.addConditionalEdges("analyze",
                edge_async(state -> state.overspent().isEmpty() ? "none" : "some"),
                Map.of("none", END, "some", "draftChange"));

        // Loop with a limit: draft again while the margin is below the policy minimum.
        graph.addConditionalEdges("draftChange",
                edge_async(state -> {
                    double margin = state.<Double>value("margin").orElse(0.0);
                    boolean tooSmall = margin < REQUIRED_MARGIN;
                    return (tooSmall && state.loops() < MAX_LOOPS) ? "again" : "ready";
                }),
                Map.of("again", "draftChange", "ready", "submit"));

        graph.addEdge("submit", END);

        return graph.compile(CompileConfig.builder()
                .checkpointSaver(new MemorySaver())
                .interruptBefore("submit")
                .build());
    }
}
