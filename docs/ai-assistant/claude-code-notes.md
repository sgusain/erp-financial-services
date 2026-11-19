# Claude Code notes (spec 13)

Draft starting point. Edit it into your own words after you have tried each item.

## Setup in this repo
- `CLAUDE.md` holds the project rules. Claude Code reads it at the start of every session.
- Skill: `.claude/skills/new-endpoint/SKILL.md`. Ask: "add an endpoint to budget-service that ..." and it follows the steps.
- Sub-agents: `.claude/agents/test-writer.md` (writes tests) and `.claude/agents/reviewer.md` (read-only review). Ask: "use test-writer on BudgetService and reviewer on my diff in parallel".

## Habits
1. One session per spec or task. Start fresh instead of continuing a very long chat.
2. Point to files by path. Say what "done" means (tests pass).
3. Run `/compact` when a long session gets slow or forgets details; restart if it keeps drifting.
4. Put repeated rules in `CLAUDE.md`, not in every prompt.
5. Let the agent run the tests and read the failure. Do not accept "done" without a green run.
6. Watch cost: long sessions and big file reads use many tokens. Keep specs short.

## Debug loop exercise
1. Break one assertion in a test on purpose.
2. Ask: "run the tests for transaction-service and fix the failure".
3. Check that it re-runs the tests after the fix. Write the result here: ____

## Java 17 refactor (not done by the spec author)
Pending in this repo: records for DTOs and events where safe, switch expressions, text blocks. Do one service per commit. Do not change JSON or Kafka event shapes. Run `mvn clean install` before each commit. This needs Maven, so run it on your machine.

## Results log
- new-endpoint skill used on: ____ result: ____
- test-writer used on: ____ result: ____
- reviewer used on: ____ result: ____
