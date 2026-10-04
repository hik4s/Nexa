ALTER TABLE transactions
    ADD COLUMN recurrence_id UUID,
    ADD COLUMN recurrence_occurrence_number INTEGER;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_recurrence
    FOREIGN KEY (recurrence_id) REFERENCES recurrences(id) ON DELETE RESTRICT;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_recurrence_occurrence_positive
    CHECK (recurrence_occurrence_number IS NULL OR recurrence_occurrence_number > 0);

ALTER TABLE transactions
    ADD CONSTRAINT transactions_recurrence_pair_complete
    CHECK ((recurrence_id IS NULL) = (recurrence_occurrence_number IS NULL));

CREATE UNIQUE INDEX uq_transactions_recurrence_occurrence
    ON transactions (recurrence_id, recurrence_occurrence_number)
    WHERE recurrence_id IS NOT NULL;
