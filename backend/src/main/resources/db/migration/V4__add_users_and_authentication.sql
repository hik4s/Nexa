CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT users_email_not_blank CHECK (length(btrim(email)) > 3),
    CONSTRAINT users_display_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT users_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE user_credentials (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(512) NOT NULL,
    password_algorithm VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_credentials_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_sessions_expiry_after_creation CHECK (expires_at > created_at),
    CONSTRAINT user_sessions_last_seen_valid CHECK (last_seen_at >= created_at)
);

CREATE TABLE password_recovery_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CONSTRAINT fk_password_recovery_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT password_recovery_expiry_after_creation CHECK (expires_at > created_at)
);

ALTER TABLE accounts ADD CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE categories ADD CONSTRAINT fk_categories_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE transfers ADD CONSTRAINT fk_transfers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE installments ADD CONSTRAINT fk_installments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE recurrences ADD CONSTRAINT fk_recurrences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE cards ADD CONSTRAINT fk_cards_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;

CREATE INDEX idx_user_sessions_user_active ON user_sessions (user_id, expires_at, revoked_at);
CREATE INDEX idx_password_recovery_user_active ON password_recovery_tokens (user_id, expires_at, used_at);
