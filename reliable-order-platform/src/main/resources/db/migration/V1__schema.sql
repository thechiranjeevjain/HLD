CREATE TABLE customer_orders(id UUID PRIMARY KEY,customer_id VARCHAR(100) NOT NULL,sku VARCHAR(100) NOT NULL,quantity INTEGER NOT NULL CHECK(quantity>0),unit_price NUMERIC(19,2) NOT NULL CHECK(unit_price>0),status VARCHAR(30) NOT NULL,idempotency_key VARCHAR(200) NOT NULL UNIQUE,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0);
CREATE INDEX idx_orders_customer_created ON customer_orders(customer_id,created_at DESC);
CREATE TABLE outbox_events(id UUID PRIMARY KEY,aggregate_type VARCHAR(100) NOT NULL,aggregate_id UUID NOT NULL,event_type VARCHAR(100) NOT NULL,payload TEXT NOT NULL,created_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ);
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
CREATE TABLE audit_records(id UUID PRIMARY KEY,actor VARCHAR(200) NOT NULL,action VARCHAR(100) NOT NULL,resource_type VARCHAR(100) NOT NULL,resource_id VARCHAR(200) NOT NULL,occurred_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_audit_resource ON audit_records(resource_type,resource_id,occurred_at);
CREATE TABLE processed_events(event_id UUID PRIMARY KEY,processed_at TIMESTAMPTZ NOT NULL);
