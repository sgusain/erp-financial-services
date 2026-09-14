CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP
);

CREATE INDEX idx_outbox_events_published_created ON outbox_events (published, created_at);
