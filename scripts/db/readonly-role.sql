-- Read-only database role for the AI slow query finder (spec 15).
--
-- Run as the postgres superuser. The password is passed on the command line,
-- never stored in git:
--   psql -h localhost -U postgres -v ai_pw='choose-a-strong-password' -f scripts/db/readonly-role.sql
--
-- The role can log in, read tables and read pg_stat_statements. It cannot write.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ai_readonly') THEN
    CREATE ROLE ai_readonly LOGIN;
  END IF;
END
$$;

ALTER ROLE ai_readonly WITH PASSWORD :'ai_pw';

-- Safety nets: every session is read-only and short.
ALTER ROLE ai_readonly SET default_transaction_read_only = on;
ALTER ROLE ai_readonly SET statement_timeout = '10s';

-- pg_monitor lets the role read pg_stat_statements and other statistics.
GRANT pg_monitor TO ai_readonly;

GRANT CONNECT ON DATABASE userdb TO ai_readonly;
GRANT CONNECT ON DATABASE accountdb TO ai_readonly;
GRANT CONNECT ON DATABASE budgetdb TO ai_readonly;
GRANT CONNECT ON DATABASE transactiondb TO ai_readonly;
GRANT CONNECT ON DATABASE notificationdb TO ai_readonly;

\connect userdb
GRANT USAGE ON SCHEMA public TO ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ai_readonly;

\connect accountdb
GRANT USAGE ON SCHEMA public TO ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ai_readonly;

\connect budgetdb
GRANT USAGE ON SCHEMA public TO ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ai_readonly;

\connect transactiondb
GRANT USAGE ON SCHEMA public TO ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ai_readonly;

\connect notificationdb
GRANT USAGE ON SCHEMA public TO ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ai_readonly;
