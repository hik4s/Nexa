CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(240) NOT NULL,
    dedupe_key VARCHAR(180) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT notifications_user_dedupe UNIQUE (user_id, dedupe_key)
);

CREATE INDEX idx_notifications_user_created
    ON notifications(user_id, created_at DESC);
