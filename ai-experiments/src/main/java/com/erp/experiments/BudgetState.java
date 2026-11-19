package com.erp.experiments;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

/**
 * The shared state of the graph. Every node reads it and returns the keys it wants to change.
 * Only simple values (numbers, text, lists of text) are used, so saving checkpoints stays easy.
 */
public class BudgetState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.of();

    public BudgetState(Map<String, Object> data) {
        super(data);
    }

    public int fiscalYear() {
        return this.<Integer>value("fiscalYear").orElse(0);
    }

    @SuppressWarnings("unchecked")
    public List<String> overspent() {
        return (List<String>) value("overspent").orElse(List.of());
    }

    public int loops() {
        return this.<Integer>value("loops").orElse(0);
    }

    public boolean approved() {
        return this.<Boolean>value("approved").orElse(false);
    }

    public String proposal() {
        return this.<String>value("proposal").orElse("");
    }

    public String result() {
        return this.<String>value("result").orElse("");
    }
}
