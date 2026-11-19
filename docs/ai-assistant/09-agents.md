# 09. Agents: Budget Analyst (Course 1 part 11, Course 2 section 10)

## Goal
An agent that analyzes the budgets of one fiscal year and drafts a budget change request, with a step limit and human approval.

## Steps
1. `BudgetAnalystAgent` with its own system prompt (`prompts/budget-analyst.st`) and the read tools from spec 08.
2. Loop control: maximum steps (default 6, config `erp.ai.agent.max-steps`), maximum tool calls, overall timeout. When a limit is hit, stop and return what was found with `status=STEP_LIMIT`.
3. `POST /api/ai/agents/budget-analyst` with `{ fiscalYear }`. Output record: `runId`, `status` (DONE, STEP_LIMIT, FAILED), `steps`, `report` (text). The report lists over-budget budgets (spent more than total, status EXCEEDED, or above 90 percent used), looks at recent transactions, and may propose one change. A budget has no category, so the analysis is per budget, not per category.
4. If the agent proposes a change, it calls `requestBudgetChange`, which stores a `pending_action` (spec 08). Never submit directly. Add a hook where an approval step can later use the Camunda branch (`feature/camunda-budget-approval`); for now approval is the confirm endpoint.
5. Persist each agent run (`agent_run` table, Flyway `V4`): user id, fiscal year in the input text, steps, status, result. Log each step.

## Done when
- A run on seeded data finds the overspend and drafts a change.
- Step limit test: after `max-steps` tool calls every tool returns LIMIT_REACHED and the run ends with `status=STEP_LIMIT`.
- No data is changed without the confirm call.

## Interview notes
- "An agent is a model with tools in a loop that continues until the goal is done. I always set a maximum number of steps and a timeout, and a human approves any action that changes data."
