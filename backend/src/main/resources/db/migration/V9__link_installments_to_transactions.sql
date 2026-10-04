-- Link scheduled installment items to their posted ledger transactions.
-- This keeps the installment plan as the source of schedule/state while each
-- installment remains a first-class card transaction and can be billed normally.

ALTER TABLE transactions
    ADD COLUMN installment_id UUID,
    ADD COLUMN installment_number INTEGER;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_installment
    FOREIGN KEY (installment_id) REFERENCES installments(id) ON DELETE RESTRICT;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_installment_metadata_check
    CHECK (
        (installment_id IS NULL AND installment_number IS NULL)
        OR (installment_id IS NOT NULL AND installment_number BETWEEN 1 AND 360)
    );

CREATE UNIQUE INDEX uq_transactions_installment_item
    ON transactions (installment_id, installment_number)
    WHERE installment_id IS NOT NULL;

CREATE INDEX idx_transactions_installment
    ON transactions (installment_id, installment_number);
