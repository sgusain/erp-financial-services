package com.erp.experiments;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j style: you write an interface, the library builds the implementation (AiServices).
 * The system message and the tools are attached to it. Compare with ChatClient in Spring AI.
 */
public interface BudgetAnalyst {

    @SystemMessage("""
            You are a budget analyst. Use the tool to read budgets.
            List every budget that is over its limit. Use only data from the tool.
            """)
    String analyze(@UserMessage String request);
}
