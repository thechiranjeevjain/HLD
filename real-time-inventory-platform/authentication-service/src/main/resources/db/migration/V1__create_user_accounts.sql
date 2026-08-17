CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(32) NOT NULL,
    provider VARCHAR(64),
    provider_subject VARCHAR(160),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_user_accounts_email ON user_accounts (LOWER(email));
CREATE UNIQUE INDEX uk_user_accounts_provider_subject
    ON user_accounts (provider, provider_subject)
    WHERE provider IS NOT NULL AND provider_subject IS NOT NULL;
