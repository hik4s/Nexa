ALTER TABLE accounts ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX accounts_one_primary_per_owner ON accounts(owner_id) WHERE is_primary = TRUE AND status = 'ACTIVE';
