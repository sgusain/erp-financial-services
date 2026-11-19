-- Load test data so the slow query finder has something to find (spec 15).
-- LOCAL DEVELOPMENT ONLY. Do not run against any shared or production database.
--
--   psql -h localhost -U postgres -d transactiondb -f scripts/db/seed-transactions.sql
--
-- The transactions table has no index on account_id, so queries that filter by
-- account_id become slow sequential scans as the table grows.

INSERT INTO transactions (account_id, amount, type, description, transaction_date, created_at)
SELECT
  (random() * 1000)::bigint + 1,
  round((random() * 5000)::numeric, 2),
  CASE WHEN random() < 0.5 THEN 'CREDIT' ELSE 'DEBIT' END,
  'Seed transaction ' || g,
  now() - (random() * 365) * interval '1 day',
  now()
FROM generate_series(1, 100000) AS g;

ANALYZE transactions;
