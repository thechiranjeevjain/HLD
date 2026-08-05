CREATE TABLE fraud_decisions (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(160) NOT NULL UNIQUE,
    user_id VARCHAR(160) NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_level VARCHAR(32) NOT NULL,
    reasons VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_fraud_decisions_user_id ON fraud_decisions (user_id);
CREATE INDEX idx_fraud_decisions_risk_level ON fraud_decisions (risk_level);
