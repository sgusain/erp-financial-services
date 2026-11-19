# n8n automation (spec 17)

## Start n8n locally
```
cp .env.n8n.example .env.n8n          # then fill in the three values
docker compose -f docker-compose.n8n.yml --env-file .env.n8n up -d
```
Open http://localhost:5678 and log in with the basic auth user you set.

## Import the starter workflow
1. In n8n: Workflows, Import from file, choose `docs/ai-assistant/n8n/github-digest.json`.
2. Open the node "Summarize With Claude". Create a credential of type **Header Auth** with name `x-api-key` and your Anthropic API key as the value. Attach it to that node. The key stays inside n8n (encrypted with `N8N_ENCRYPTION_KEY`).
3. Click "Execute workflow". Open "Digest Output" to see the five-bullet digest.

The starter uses plain HTTP Request nodes (GitHub public API, then Anthropic API), because they are the same in every n8n version. It was written without being run in this repo's build environment, so if a node shows a parameter error after import, fix it in the UI and re-export.

## Step 2 of the spec: schedule and error branch
- Replace "Manual Trigger" with a **Schedule Trigger** (weekly).
- Add an **Error Trigger** workflow or the node's "On Error: Continue (using error output)" setting, and send yourself a message when a run fails (email or Slack node).
- Force a failure (wrong URL) to confirm the error branch fires.

## Export safely
Before committing an exported workflow, check it has no credentials or tokens: `grep -nEi "(sk-ant|ghp_|github_pat_|api[_-]?key\"?\s*:)" docs/ai-assistant/n8n/*.json` should return nothing.

## n8n or code?
- Use n8n for glue between tools, quick automations, and things that change often (digests, notifications, simple syncs).
- Write code for core business rules, anything that needs unit tests, code review and version control, and anything on the money path.
- A good test: if a bug would cost money or data, write it in code with tests.
