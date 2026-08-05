CREATE TABLE IF NOT EXISTS order_events (
    order_id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    price NUMERIC(19, 4) NOT NULL,
    notional NUMERIC(19, 4) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS exposures (
    exposure_id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    net_quantity BIGINT NOT NULL,
    gross_notional NUMERIC(19, 4) NOT NULL,
    daily_exposure NUMERIC(19, 4) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_exposures_client_symbol UNIQUE (client_id, symbol)
);

CREATE INDEX IF NOT EXISTS idx_order_events_client_symbol ON order_events (client_id, symbol);
CREATE INDEX IF NOT EXISTS idx_exposures_client_symbol ON exposures (client_id, symbol);
