CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(255) NOT NULL UNIQUE,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance NUMERIC(38, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP
);
