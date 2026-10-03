ALTER TABLE transactions
    DROP CONSTRAINT transactions_source_check;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_source_check
    CHECK (source IN ('MANUAL', 'IMPORTED', 'OPEN_FINANCE', 'SYSTEM'));
