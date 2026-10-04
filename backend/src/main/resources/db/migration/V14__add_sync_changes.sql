CREATE SEQUENCE sync_change_cursor_seq;

CREATE TABLE sync_changes (
    cursor BIGINT PRIMARY KEY DEFAULT nextval('sync_change_cursor_seq'),
    user_id UUID NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('UPSERT', 'DELETE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_changes_user_cursor ON sync_changes (user_id, cursor);

CREATE OR REPLACE FUNCTION record_transaction_sync_change()
RETURNS TRIGGER AS $$
DECLARE
    owner UUID;
BEGIN
    SELECT a.owner_id INTO owner FROM accounts a WHERE a.id = COALESCE(NEW.account_id, OLD.account_id);
    INSERT INTO sync_changes (user_id, entity_type, entity_id, operation)
    VALUES (owner, 'TRANSACTION', COALESCE(NEW.id, OLD.id), CASE WHEN TG_OP = 'DELETE' THEN 'DELETE' ELSE 'UPSERT' END);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transactions_sync_change
AFTER INSERT OR UPDATE OR DELETE ON transactions
FOR EACH ROW EXECUTE FUNCTION record_transaction_sync_change();
