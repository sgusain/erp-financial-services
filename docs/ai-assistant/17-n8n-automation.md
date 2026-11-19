# 17. n8n automation (Course 2 section 15)

## Goal
Run n8n locally with Docker and build a GitHub activity digest workflow with an AI step.

## Steps
1. Add `docker-compose.n8n.yml` (separate file, not part of the main compose): n8n image, port 5678, a named volume, `N8N_ENCRYPTION_KEY` and basic auth from environment variables. Document the start command in `docs/ai-assistant/n8n.md`.
2. Workflow 1 "GitHub Activity Digest": manual trigger, GitHub node (commits and merged PRs for the last 7 days for this repo), AI node (summarize in 5 bullets), then email or Slack node (or just output). Export the workflow JSON to `docs/ai-assistant/n8n/github-digest.json` with credentials removed.
3. Workflow 2: replace the manual trigger with a schedule (weekly). Add an error branch that notifies you when a run fails.
4. Write 8 lines in `docs/ai-assistant/n8n.md` on when to use n8n and when to write code: use n8n for glue between tools and quick automations; write code for core business logic that needs tests, reviews and version control.

## Done when
- n8n runs from compose and the digest workflow runs end to end.
- The exported JSON contains no credentials or tokens.
- A forced failure triggers the error branch.

## Interview notes
- "For simple automation between tools I would use n8n. For core business rules I would write code, because it needs tests and version control."
