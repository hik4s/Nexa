-- Performance indexes for high-volume transaction history and related-transaction checks.
-- The previous account/date index is replaced by the covering ordering index.
DROP INDEX IF EXISTS idx_transactions_account_date;

CREATE INDEX idx_transactions_account_date_created_id
    ON transactions (account_id, occurred_on DESC, created_at DESC, id DESC);

CREATE INDEX idx_transactions_related_type_active
    ON transactions (related_transaction_id, type)
    WHERE status <> 'CANCELLED';
