# Slow query finder setup (spec 15)

Goal: an AI agent explores the ERP PostgreSQL databases read-only, finds slow queries and suggests indexes. You apply the fix yourself.

## 1. Start Postgres with pg_stat_statements (local only)

```
docker compose -f docker-compose.yml -f docker-compose.slowquery.yml up -d postgres
psql -h localhost -U postgres -d transactiondb -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
```

## 2. Create the read-only role

```
psql -h localhost -U postgres -v ai_pw='choose-a-strong-password' -f scripts/db/readonly-role.sql
```

The role can read tables and statistics. It cannot write, and every query is limited to 10 seconds.

## 3. Add load

```
psql -h localhost -U postgres -d transactiondb -f scripts/db/seed-transactions.sql
```

This adds 100,000 transactions. The `transactions` table has no index on `account_id`, so account lookups become slow scans. Call the transaction list endpoints a few times so `pg_stat_statements` records them.

## 4. Connect a Postgres MCP server

Copy `.mcp.json.example` to `.mcp.local.json` (gitignored) and fill in the connection string with the `ai_readonly` user. Use a currently maintained Postgres MCP server and check its README for the exact package name and read-only mode before you install it. Prefer one that supports a restricted or read-only mode, and keep the database role read-only as the real protection.

## 5. Run the agent

Use the task in `prompts/slow-query-finder.md`. Save the report it produces as `docs/ai-assistant/reports/slow-query-report.md`.

## 6. Apply one fix yourself

Add a Flyway migration in `transaction-service` (next `V` number), for example an index on `transactions (account_id, transaction_date)`. Put the before and after `EXPLAIN` in the pull request description.

## Safety checklist
- [ ] Role is `ai_readonly`. Try an `INSERT` as that role and confirm it fails.
- [ ] No password or connection string is committed.
- [ ] The seed script was only run on your local database.
