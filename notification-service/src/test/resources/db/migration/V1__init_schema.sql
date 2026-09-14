CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN,
    created_at TIMESTAMP
);
