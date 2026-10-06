CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    event_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(80),
    target_id UUID,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_actor_created
    ON audit_events (actor_user_id, created_at DESC);

CREATE INDEX idx_audit_events_type_created
    ON audit_events (event_type, created_at DESC);

CREATE INDEX idx_audit_events_target
    ON audit_events (target_type, target_id, created_at DESC);