# 18. LangChain4j and LangGraph4j (extra, in neither course)

## Goal
Rebuild the Budget Analyst agent with LangChain4j, then as a LangGraph4j graph, and compare with Spring AI. This is for interviews that ask about these frameworks.

## Steps
1. Put this in a separate small module `ai-experiments/` (standalone, not in root modules, not deployed). Verify current versions of LangChain4j and LangGraph4j before use; do not guess.
2. **LangChain4j version**: define an `AiServices` interface for the agent with system message, tool methods (same read tools, calling the ERP through the same Feign clients or simple stubs), and chat memory. Run the same budget analysis input and compare output.
3. **LangGraph4j version**: model the flow as a graph with shared state: `fetchData` then `analyze` then `draftChange` then `humanApproval` (interrupt) then `submit`. Add a conditional edge: if no overspend, end early. Add a loop limit.
4. Write `docs/ai-assistant/framework-comparison.md` (your own words, 15 lines): when Spring AI, when LangChain4j, when LangGraph4j; lines of code, testability, state handling, human-in-the-loop support.

## Done when
- Both versions produce the same finding on the same sample data.
- The graph pauses at human approval and continues after an approve call.
- Comparison note written.

## Interview notes
- "I use Spring AI because my stack is Spring. If I needed a stateful multi-step agent with loops and human approval, I would look at LangGraph4j."
