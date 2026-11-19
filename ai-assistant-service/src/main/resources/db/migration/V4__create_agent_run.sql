-- One row per Budget Analyst agent run (for audit and debugging).
CREATE TABLE IF NOT EXISTS agent_run (
    id          UUID PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    agent_name  VARCHAR(50)  NOT NULL,
    input_text  VARCHAR(500) NOT NULL,
    steps       INT          NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    result_text TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_agent_run_user ON agent_run (username, created_at DESC);
