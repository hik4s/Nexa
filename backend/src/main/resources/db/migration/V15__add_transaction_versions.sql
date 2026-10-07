ALTER TABLE transactions
    ADD COLUMN version BIGINT NOT NULL DEFAULT 1;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_version_positive CHECK (version > 0);

CREATE INDEX idx_transactions_version ON transactions (id, version);
