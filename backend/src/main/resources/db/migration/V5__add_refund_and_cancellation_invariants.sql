-- Financial reversal invariants.
-- A posted transaction may have at most one accounting cancellation.
CREATE UNIQUE INDEX ux_transactions_one_cancellation_per_original
    ON transactions (related_transaction_id)
    WHERE type = 'CANCELLATION';

CREATE INDEX idx_transactions_related_type_status
    ON transactions (related_transaction_id, type, status);
