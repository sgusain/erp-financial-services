# Spring AI vs LangChain4j vs LangGraph4j (draft, edit in your own words)

Same job in all three: the Budget Analyst. Code is in `ai-experiments/` (LangChain4j, LangGraph4j) and `ai-assistant-service/` (Spring AI).

- **Spring AI** is my choice when the app is already Spring Boot. Config, security, Eureka and tests all work the way I know. `ChatClient`, `@Tool`, advisors and memory are one starter away.
- **LangChain4j** is plain Java, not tied to Spring. You write an interface and `AiServices` builds it. Less code for a simple agent, good if the app is not Spring.
- **LangGraph4j** is for flows with state: steps, loops, branches, and pause for a human. The graph saves its state, so it can stop and continue later.
- **Lines of code:** LangChain4j agent is shortest (an interface plus tools). Spring AI is a bit longer but has the Spring parts built in. The graph is longest, but the flow is easy to see.
- **Testability:** all three can use a stub model. In the graph, each node is a small function, so it is easy to test alone.
- **State:** Spring AI and LangChain4j keep chat memory only. LangGraph4j keeps the whole workflow state with checkpoints.
- **Human in the loop:** In Spring AI I built it by hand (pending action plus confirm endpoint). In LangGraph4j it is a feature: interrupt before a node, update the state, resume.
- **My rule:** Spring app, normal agent -> Spring AI. Stateful multi-step flow with approval and loops -> look at LangGraph4j. Not on Spring -> LangChain4j.
- **Caution:** the ERP agent here is a short loop with a step limit, so Spring AI is enough. I did not need a graph for it.
