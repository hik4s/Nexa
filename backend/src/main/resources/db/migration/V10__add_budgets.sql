CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    start_on DATE NOT NULL,
    end_on DATE NOT NULL,
    description VARCHAR(160),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT budgets_period_valid CHECK (end_on >= start_on),
    CONSTRAINT budgets_description_not_blank CHECK (description IS NULL OR length(btrim(description)) > 0),
    CONSTRAINT budgets_description_length CHECK (description IS NULL OR length(description) <= 160),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT budgets_updated_after_created CHECK (updated_at >= created_at)
);

CREATE INDEX idx_budgets_user_period ON budgets (user_id, start_on, end_on, status);
CREATE INDEX idx_budgets_user_category_period ON budgets (user_id, category_id, start_on, end_on, status);
