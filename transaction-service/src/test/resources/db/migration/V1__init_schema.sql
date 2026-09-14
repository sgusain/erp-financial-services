CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount NUMERIC(38,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    transaction_date TIMESTAMP,
    created_at TIMESTAMP
);
