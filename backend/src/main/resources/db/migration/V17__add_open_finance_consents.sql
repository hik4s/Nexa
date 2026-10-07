CREATE TABLE open_finance_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider_id VARCHAR(80) NOT NULL,
    external_consent_id VARCHAR(255),
    scopes_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (
        status IN (
            'REQUESTED',
            'AWAITING_AUTHORIZATION',
            'ACTIVE',
            'EXPIRED',
            'REVOKED',
            'FAILED'
        )
    ),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT open_finance_consents_external_unique
        UNIQUE (provider_id, external_consent_id),
    CONSTRAINT open_finance_consents_updated_after_created
        CHECK (updated_at >= created_at),
    CONSTRAINT open_finance_consents_expiry_valid
        CHECK (expires_at IS NULL OR expires_at >= created_at),
    CONSTRAINT open_finance_consents_revoked_valid
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_open_finance_consents_user_status
    ON open_finance_consents (user_id, status, updated_at DESC);

CREATE INDEX idx_open_finance_consents_external
    ON open_finance_consents (provider_id, external_consent_id);
