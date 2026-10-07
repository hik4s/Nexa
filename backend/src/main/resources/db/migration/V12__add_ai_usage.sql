CREATE TABLE ai_usage_daily (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, usage_date)
);
