-- Base schema for integration tests running against a fresh Testcontainers Postgres
-- instance. In real deployments the `accounts` table predates the Flyway history
-- (baselined at version 1 via spring.flyway.baseline-version=1), so no V1 migration
-- ships in src/main/resources. Tests start from a completely empty database, so we
-- provide the equivalent base schema here, on the test classpath only, so that the
-- real V2__add_version_to_accounts.sql (from src/main/resources) can run on top of it
-- exactly as it does in production.
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(255) NOT NULL UNIQUE,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance NUMERIC(38,2),
    created_at TIMESTAMP
);
