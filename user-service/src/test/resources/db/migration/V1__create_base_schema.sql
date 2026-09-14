-- Base schema for integration tests running against a fresh Testcontainers Postgres
-- instance. In real deployments the `users` table predates the Flyway history
-- (baselined at version 1 via spring.flyway.baseline-version=1), so no V1 migration
-- ships in src/main/resources. Tests start from a completely empty database, so we
-- provide the equivalent base schema here, on the test classpath only, so that the
-- real V2__create_refresh_tokens.sql (from src/main/resources) can run on top of it
-- exactly as it does in production.
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP
);
