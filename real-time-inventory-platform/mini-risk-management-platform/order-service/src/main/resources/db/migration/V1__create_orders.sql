CREATE TABLE orders (
    id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    price NUMERIC(19, 4) NOT NULL CHECK (price > 0),
    notional NUMERIC(19, 4) NOT NULL CHECK (notional > 0),
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_orders_client_symbol_created_at ON orders (client_id, symbol, created_at DESC);
CREATE INDEX idx_orders_status_created_at ON orders (status, created_at DESC);

