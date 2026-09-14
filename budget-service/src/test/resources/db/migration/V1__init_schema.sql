CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    budget_name VARCHAR(255) NOT NULL,
    total_amount NUMERIC(38,2),
    spent_amount NUMERIC(38,2),
    fiscal_year INTEGER NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP
);
