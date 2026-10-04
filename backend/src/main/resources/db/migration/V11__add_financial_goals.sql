CREATE TABLE goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(80) NOT NULL,
    target_amount_minor BIGINT NOT NULL CHECK (target_amount_minor > 0),
    current_amount_minor BIGINT NOT NULL CHECK (current_amount_minor >= 0 AND current_amount_minor <= target_amount_minor),
    currency VARCHAR(3) NOT NULL,
    target_date DATE,
    description VARCHAR(160),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_goals_user_status_date
    ON goals(user_id, status, target_date, created_at DESC);

CREATE TABLE goal_contributions (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL,
    occurred_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_goal_contributions_goal
    ON goal_contributions(goal_id, occurred_on DESC, created_at DESC);

CREATE INDEX idx_goal_contributions_user
    ON goal_contributions(user_id, occurred_on DESC);
