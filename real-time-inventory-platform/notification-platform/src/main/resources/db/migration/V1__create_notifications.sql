CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(240),
    body VARCHAR(4000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    max_attempts INTEGER NOT NULL CHECK (max_attempts > 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(1000),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE dead_letter_notifications (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL UNIQUE REFERENCES notifications (id),
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notifications_due
    ON notifications (status, next_attempt_at, created_at);
