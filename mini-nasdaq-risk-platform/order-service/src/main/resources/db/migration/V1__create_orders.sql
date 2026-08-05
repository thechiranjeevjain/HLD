CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    price NUMERIC(19, 4) NOT NULL,
    notional NUMERIC(19, 4) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_client_symbol ON orders (client_id, symbol);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at DESC);
