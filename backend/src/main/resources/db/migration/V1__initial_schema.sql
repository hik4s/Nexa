-- NEXA Finance initial schema.
-- owner_id/user_id FKs are intentionally deferred until the User aggregate is modeled in Part 1.7.

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'WALLET', 'INVESTMENT', 'OTHER')),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    opening_balance_minor BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT accounts_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT accounts_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    owner_id UUID,
    name VARCHAR(60) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'BOTH')),
    parent_id UUID,
    icon_key VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT categories_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT categories_updated_after_created CHECK (updated_at >= created_at),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'PURCHASE', 'PAYMENT', 'REFUND', 'CANCELLATION', 'ADJUSTMENT')),
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('PENDING', 'POSTED', 'CANCELLED')),
    occurred_on DATE NOT NULL,
    description VARCHAR(160),
    category_id UUID,
    source VARCHAR(20) NOT NULL CHECK (source IN ('MANUAL', 'SYSTEM', 'IMPORT', 'OPEN_FINANCE', 'AI')),
    related_transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_related FOREIGN KEY (related_transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT,
    CONSTRAINT transactions_description_not_blank CHECK (description IS NULL OR length(btrim(description)) > 0),
    CONSTRAINT transactions_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    occurred_on DATE NOT NULL,
    source_transaction_id UUID NOT NULL UNIQUE,
    destination_transaction_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'POSTED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_transfers_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfers_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfers_source_transaction FOREIGN KEY (source_transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfers_destination_transaction FOREIGN KEY (destination_transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT,
    CONSTRAINT transfers_distinct_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT transfers_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE installments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    total_amount_minor BIGINT NOT NULL CHECK (total_amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    installment_count INTEGER NOT NULL CHECK (installment_count BETWEEN 2 AND 360),
    first_due_on DATE NOT NULL,
    description VARCHAR(160),
    category_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_installments_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT installments_description_not_blank CHECK (description IS NULL OR length(btrim(description)) > 0),
    CONSTRAINT installments_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE installment_items (
    installment_id UUID NOT NULL,
    number INTEGER NOT NULL CHECK (number > 0),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    due_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    PRIMARY KEY (installment_id, number),
    CONSTRAINT fk_installment_items_installment FOREIGN KEY (installment_id) REFERENCES installments(id) ON DELETE RESTRICT
);

CREATE TABLE recurrences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    frequency VARCHAR(10) NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    interval_value INTEGER NOT NULL CHECK (interval_value BETWEEN 1 AND 365),
    start_on DATE NOT NULL,
    end_on DATE,
    max_occurrences INTEGER CHECK (max_occurrences BETWEEN 1 AND 10000),
    description VARCHAR(160),
    category_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_recurrences_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurrences_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT recurrences_end_after_start CHECK (end_on IS NULL OR end_on >= start_on),
    CONSTRAINT recurrences_description_not_blank CHECK (description IS NULL OR length(btrim(description)) > 0),
    CONSTRAINT recurrences_updated_after_created CHECK (updated_at >= created_at)
);

CREATE INDEX idx_accounts_owner_status ON accounts (owner_id, status);
CREATE INDEX idx_categories_owner_status_order ON categories (owner_id, status, sort_order);
CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_transactions_account_date ON transactions (account_id, occurred_on DESC);
CREATE INDEX idx_transactions_category_date ON transactions (category_id, occurred_on DESC);
CREATE INDEX idx_transactions_related ON transactions (related_transaction_id);
CREATE INDEX idx_transfers_user_date ON transfers (user_id, occurred_on DESC);
CREATE INDEX idx_transfers_source_account_date ON transfers (source_account_id, occurred_on DESC);
CREATE INDEX idx_transfers_destination_account_date ON transfers (destination_account_id, occurred_on DESC);
CREATE INDEX idx_installments_user_status ON installments (user_id, status);
CREATE INDEX idx_installment_items_due_date ON installment_items (installment_id, due_on);
CREATE INDEX idx_recurrences_user_status ON recurrences (user_id, status);
CREATE INDEX idx_recurrences_account_status ON recurrences (account_id, status);
