CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(14, 2) NOT NULL CHECK (price >= 0),
    currency CHAR(3) NOT NULL,
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_products_sku ON products (LOWER(sku));
CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_name ON products (LOWER(name));
