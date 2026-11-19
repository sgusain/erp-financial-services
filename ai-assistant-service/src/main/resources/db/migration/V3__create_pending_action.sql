-- Write actions proposed by the assistant. Nothing changes in the ERP until the
-- user confirms (POST /api/ai/actions/{id}/confirm).
CREATE TABLE IF NOT EXISTS pending_action (
    id               UUID PRIMARY KEY,
    username         VARCHAR(100)  NOT NULL,
    action_type      VARCHAR(50)   NOT NULL,
    budget_id        BIGINT        NOT NULL,
    new_total_amount NUMERIC(38,2) NOT NULL,
    reason           VARCHAR(500)  NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP     NOT NULL DEFAULT now(),
    decided_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pending_action_user ON pending_action (username, status);
