CREATE TABLE short_links (
    id UUID PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    owner_key VARCHAR(160),
    click_count BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_short_links_owner_key ON short_links (owner_key);
CREATE INDEX idx_short_links_expires_at ON short_links (expires_at);
