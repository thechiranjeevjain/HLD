CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    price NUMERIC(14, 2) NOT NULL CHECK (price >= 0),
    currency CHAR(3) NOT NULL,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_lines (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(14, 2) NOT NULL CHECK (line_total >= 0)
);

CREATE TABLE payment_records (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders (id),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(160) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_customer_orders_customer_id ON customer_orders (customer_id);
CREATE INDEX idx_order_lines_order_id ON order_lines (order_id);
CREATE INDEX idx_payment_records_order_id ON payment_records (order_id);
