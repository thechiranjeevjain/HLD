CREATE TABLE risk_limits (
    id UUID PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    max_order_quantity BIGINT NOT NULL CHECK (max_order_quantity > 0),
    max_position_quantity BIGINT NOT NULL CHECK (max_position_quantity > 0),
    max_daily_exposure NUMERIC(19, 4) NOT NULL CHECK (max_daily_exposure > 0),
    CONSTRAINT uk_risk_limits_client_symbol UNIQUE (client_id, symbol)
);

CREATE INDEX idx_risk_limits_client_symbol ON risk_limits (client_id, symbol);

INSERT INTO risk_limits (id, client_id, symbol, max_order_quantity, max_position_quantity, max_daily_exposure)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'CLIENT-A', 'AAPL', 5000, 15000, 1000000.0000),
    ('22222222-2222-2222-2222-222222222222', 'CLIENT-A', 'MSFT', 3000, 10000, 750000.0000),
    ('33333333-3333-3333-3333-333333333333', 'CLIENT-B', 'AAPL', 1000, 5000, 250000.0000);

