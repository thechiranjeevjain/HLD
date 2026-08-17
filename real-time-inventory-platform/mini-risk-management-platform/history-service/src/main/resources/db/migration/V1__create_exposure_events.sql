CREATE TABLE exposure_events (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    price NUMERIC(19, 4) NOT NULL CHECK (price > 0),
    notional NUMERIC(19, 4) NOT NULL CHECK (notional > 0),
    status VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_exposure_client_symbol_time ON exposure_events (client_id, symbol, occurred_at DESC);
CREATE INDEX idx_exposure_client_time ON exposure_events (client_id, occurred_at DESC);

