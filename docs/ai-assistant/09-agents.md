# 09. Agents: Budget Analyst (Course 1 part 11, Course 2 section 10)

## Goal
An agent that analyzes a month of spending and drafts a budget change request, with a step limit and human approval.

## Steps
1. `BudgetAnalystAgent` with its own system prompt (`prompts/budget-analyst.st`) and the read tools from spec 08.
2. Loop control: maximum steps (default 6, config `erp.ai.agent.max-steps`), maximum tool calls, overall timeout. When a limit is hit, stop and return what was found with a flag `incomplete=true`.
3. `POST /api/ai/agents/budget-analysis` with `{ month }`. Output record: `findings` (list), `overspentCategories`, `proposedChange` (optional: budgetId, newAmount, reason).
4. If `proposedChange` exists, store it as a `pending_action` (spec 08). Never submit directly. Add a hook where an approval step can later use the Camunda branch (`feature/camunda-budget-approval`); for now approval is the confirm endpoint.
5. Persist each agent run (`agent_run` table, Flyway `V4`): user id, month, steps, tokens, status, result. Log each step.

## Done when
- A run on seeded data finds the overspend and drafts a change.
- Step limit test: with `max-steps=1` the run ends with `incomplete=true`.
- No data is changed without the confirm call.

## Interview notes
- "An agent is a model with tools in a loop that continues until the goal is done. I always set a maximum number of steps and a timeout, and a human approves any action that changes data."
