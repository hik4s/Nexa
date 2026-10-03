-- Credit cards are instruments backed by liability accounts.
-- Purchases remain normal ledger transactions; card and invoice metadata only enrich them.

ALTER TABLE accounts
    DROP CONSTRAINT accounts_type_check;

ALTER TABLE accounts
    ADD CONSTRAINT accounts_type_check
    CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'WALLET', 'INVESTMENT', 'CREDIT_CARD', 'OTHER'));

CREATE TABLE cards (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    liability_account_id UUID NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    brand VARCHAR(20) NOT NULL CHECK (brand IN ('VISA', 'MASTERCARD', 'ELO', 'AMEX', 'HIPERCARD', 'OTHER')),
    last_four_digits CHAR(4) NOT NULL CHECK (last_four_digits ~ '^[0-9]{4}$'),
    credit_limit_minor BIGINT NOT NULL CHECK (credit_limit_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    closing_day INTEGER NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_cards_liability_account FOREIGN KEY (liability_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT cards_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT cards_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_on DATE NOT NULL,
    total_amount_minor BIGINT NOT NULL CHECK (total_amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    paid_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'OVERDUE', 'PAID', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_invoices_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE RESTRICT,
    CONSTRAINT invoices_period_valid CHECK (period_end >= period_start),
    CONSTRAINT invoices_due_after_period CHECK (due_on >= period_end),
    CONSTRAINT invoices_paid_not_over_total CHECK (paid_amount_minor <= total_amount_minor),
    CONSTRAINT invoices_updated_after_created CHECK (updated_at >= created_at)
);

ALTER TABLE transactions
    ADD COLUMN card_id UUID,
    ADD COLUMN invoice_id UUID;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_transactions_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE RESTRICT;

CREATE INDEX idx_cards_owner_status ON cards (owner_id, status);
CREATE INDEX idx_cards_liability_account ON cards (liability_account_id);
CREATE INDEX idx_invoices_card_period ON invoices (card_id, period_end DESC);
CREATE INDEX idx_invoices_card_status ON invoices (card_id, status);

CREATE TABLE invoice_payment_transactions (
    invoice_id UUID NOT NULL,
    transaction_id UUID NOT NULL UNIQUE,
    PRIMARY KEY (invoice_id, transaction_id),
    CONSTRAINT fk_invoice_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_payment_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT
);
CREATE INDEX idx_transactions_card_date ON transactions (card_id, occurred_on DESC);
CREATE INDEX idx_transactions_invoice ON transactions (invoice_id);
