# 15. Slow query finder agent (Course 2 section 14)

## Goal
An agent that explores the ERP PostgreSQL databases read-only, finds slow queries and suggests indexes. (The course uses MySQL; this repo uses PostgreSQL.)

## Steps
1. Create a read-only role `ai_readonly` (SQL script in `scripts/db/readonly-role.sql`): `CONNECT` and `SELECT` only, plus `pg_monitor` for statistics. Password from environment, never committed.
2. Enable `pg_stat_statements` for local compose Postgres (`shared_preload_libraries` in compose command, then `CREATE EXTENSION`). Document it in `docs/ai-assistant/slow-query-finder.md`.
3. Configure a Postgres MCP server (check the current maintained one) pointing at the read-only role, in a local-only config file that is gitignored. Provide `docs/ai-assistant/slow-query-finder.md` with the config template using placeholders.
4. Agent prompt (`docs/ai-assistant/prompts/slow-query-finder.md`): list top queries by total time, run `EXPLAIN` (and `EXPLAIN ANALYZE` only for SELECT), find sequential scans on large tables, propose indexes as SQL in a report. Never run DDL or DML. Output a markdown report.
5. Generate load: a script that calls list endpoints with many rows (seed 100k transactions) so there is something to find.
6. Apply one suggested index yourself as a Flyway migration in the owning service, and show before and after `EXPLAIN` in the PR description.

## Done when
- The agent produces a report naming at least one real slow query and a proposed index.
- Attempting a write through the agent fails because of the role.
- No credentials in git.

## Interview notes
- "I would give the agent a read-only database user, let it find slow queries and suggest indexes, and apply the change myself through a migration after review."
