# 13. Claude Code workflow (Course 2 sections 4, 8)

## Goal
Use Claude Code properly on this repo: memory file, skills, sub-agents, context habits, debug loop, AI refactor.

## Steps
1. `CLAUDE.md` already exists at the repo root. Review it, fix anything wrong, and add a short "Gotchas" section from your own experience (Testcontainers on Windows, JDK 25 and Mockito versions, Dockerfile reactor rule).
2. Create one project skill in `.claude/skills/new-endpoint/SKILL.md`: steps to add a REST endpoint (controller, service, DTO with validation, exception handling, Pageable if list, unit test, integration test, Swagger). Use it once on a small real endpoint.
3. Create two sub-agents in `.claude/agents/`: `test-writer` (writes missing tests for a given class) and `reviewer` (read-only, reviews a diff for bugs, security, missing tests). Run both in parallel on one change.
4. Context habits (write in `docs/ai-assistant/claude-code-notes.md`, 10 lines): one session per spec, compact or restart when the context is long, point to files by path, keep specs short, watch token cost.
5. Debug loop: break one test on purpose, ask Claude Code to run `mvn -pl <module> -am verify`, read the failure and fix it. Confirm it re-runs tests before claiming success.
6. AI refactor: add the Java 17 features still pending in this repo (records for DTOs and events where safe, switch expressions, text blocks). Small commits per service. Existing tests must stay green (107 tests). Do not change JSON shapes of Kafka events or REST responses.

## Done when
- Skill and both sub-agents exist and each was used at least once (note the result in the notes file).
- Refactor PR is green on CI and the API/Kafka payloads are unchanged (add a contract-style assertion where one is missing).

## Interview notes
- "I keep project rules in a CLAUDE.md file, use a fresh session per task to save context, and let the agent run the tests and fix failures. I review every change myself."
