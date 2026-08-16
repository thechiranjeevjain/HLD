CREATE TABLE IF NOT EXISTS idempotency_requests (
  idempotency_key VARCHAR(120) PRIMARY KEY, request_hash VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NOT NULL, response_json TEXT NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS payments (
  id VARCHAR(64) PRIMARY KEY, customer_id VARCHAR(64) NOT NULL, order_id VARCHAR(64) NOT NULL,
  amount BIGINT NOT NULL, currency VARCHAR(3) NOT NULL, settlement_currency VARCHAR(3) NOT NULL,
  status VARCHAR(32) NOT NULL, state_rank INT NOT NULL, stripe_payment_intent_id VARCHAR(80) NOT NULL UNIQUE,
  stripe_charge_id VARCHAR(80), failure_reason VARCHAR(240), created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS webhook_events (
  event_id VARCHAR(100) PRIMARY KEY, event_type VARCHAR(80) NOT NULL, object_id VARCHAR(100) NOT NULL,
  payload_hash VARCHAR(64) NOT NULL, status VARCHAR(24) NOT NULL, received_at TIMESTAMP WITH TIME ZONE NOT NULL,
  processed_at TIMESTAMP WITH TIME ZONE, note VARCHAR(240), stripe_object_id VARCHAR(100), amount BIGINT,
  currency VARCHAR(3), settlement_amount BIGINT, settlement_currency VARCHAR(3), fx_rate VARCHAR(40), reason VARCHAR(240), attempt INT DEFAULT 0,
  next_run_at TIMESTAMP WITH TIME ZONE
);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS stripe_object_id VARCHAR(100);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS amount BIGINT;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS settlement_amount BIGINT;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS settlement_currency VARCHAR(3);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS fx_rate VARCHAR(40);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS reason VARCHAR(240);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS attempt INT DEFAULT 0;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMP WITH TIME ZONE;
CREATE TABLE IF NOT EXISTS ledger_entries (
  id VARCHAR(64) PRIMARY KEY, journal_id VARCHAR(64) NOT NULL, account_id VARCHAR(100) NOT NULL,
  currency VARCHAR(3) NOT NULL, amount BIGINT NOT NULL, entry_type VARCHAR(40) NOT NULL,
  ref_id VARCHAR(100) NOT NULL, stripe_object_id VARCHAR(100), description VARCHAR(240) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ledger_ref ON ledger_entries(ref_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created ON ledger_entries(created_at, currency);
CREATE TABLE IF NOT EXISTS balance_projection (
  account_id VARCHAR(100) NOT NULL, currency VARCHAR(3) NOT NULL, balance BIGINT NOT NULL,
  version BIGINT NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, PRIMARY KEY(account_id,currency)
);
CREATE TABLE IF NOT EXISTS external_transactions (
  external_id VARCHAR(100) PRIMARY KEY, match_key VARCHAR(120) NOT NULL, amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL, status VARCHAR(30) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  raw_hash VARCHAR(64) NOT NULL, source VARCHAR(30) NOT NULL
);
CREATE TABLE IF NOT EXISTS reconciliation_runs (
  id VARCHAR(64) PRIMARY KEY, source VARCHAR(30) NOT NULL, range_start DATE NOT NULL, range_end DATE NOT NULL,
  status VARCHAR(30) NOT NULL, attempt INT NOT NULL, run_key VARCHAR(120) UNIQUE,
  started_at TIMESTAMP WITH TIME ZONE, completed_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS reconciliation_items (
  run_id VARCHAR(64) NOT NULL, match_key VARCHAR(120) NOT NULL, internal_sum BIGINT NOT NULL,
  external_sum BIGINT NOT NULL, delta BIGINT NOT NULL, currency VARCHAR(3) NOT NULL,
  state VARCHAR(30) NOT NULL, reason VARCHAR(240) NOT NULL, last_error VARCHAR(240),
  PRIMARY KEY(run_id,match_key), FOREIGN KEY(run_id) REFERENCES reconciliation_runs(id)
);
CREATE TABLE IF NOT EXISTS reconciliation_shards (
  run_id VARCHAR(64) NOT NULL, shard_id INT NOT NULL, status VARCHAR(30) NOT NULL, cursor_key VARCHAR(120),
  attempt INT NOT NULL, next_run_at TIMESTAMP WITH TIME ZONE, last_error VARCHAR(240),
  started_at TIMESTAMP WITH TIME ZONE, completed_at TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY(run_id,shard_id), FOREIGN KEY(run_id) REFERENCES reconciliation_runs(id)
);
CREATE INDEX IF NOT EXISTS idx_shard_ready ON reconciliation_shards(status,next_run_at);
CREATE TABLE IF NOT EXISTS outbox_events (
  id VARCHAR(64) PRIMARY KEY, event_type VARCHAR(60) NOT NULL, aggregate_id VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, published_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE IF NOT EXISTS external_imports (
  id VARCHAR(64) PRIMARY KEY, source VARCHAR(30) NOT NULL, file_name VARCHAR(200) NOT NULL, file_hash VARCHAR(64) NOT NULL UNIQUE,
  schema_version VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, accepted_rows INT NOT NULL, quarantined_rows INT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS quarantined_external_rows (
  id VARCHAR(64) PRIMARY KEY, import_id VARCHAR(64) NOT NULL, row_number INT NOT NULL, raw_payload TEXT NOT NULL,
  error_code VARCHAR(60) NOT NULL, error_detail VARCHAR(240) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS adjustment_requests (
  id VARCHAR(64) PRIMARY KEY, reconciliation_id VARCHAR(64) NOT NULL, reason VARCHAR(240) NOT NULL,
  entries_json TEXT NOT NULL, total_absolute BIGINT NOT NULL, status VARCHAR(30) NOT NULL,
  requested_at TIMESTAMP WITH TIME ZONE NOT NULL, approved_by VARCHAR(80), approved_at TIMESTAMP WITH TIME ZONE, journal_id VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS audit_events (
  id VARCHAR(64) PRIMARY KEY, event_type VARCHAR(60) NOT NULL, aggregate_id VARCHAR(100) NOT NULL,
  details TEXT NOT NULL, correlation_id VARCHAR(100), created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
