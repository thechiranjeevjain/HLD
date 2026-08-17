CREATE TABLE scheduler_locks (
    lock_name VARCHAR(120) PRIMARY KEY,
    owner_id VARCHAR(160) NOT NULL,
    locked_until TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE scheduled_jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    payload VARCHAR(4000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    run_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    max_attempts INTEGER NOT NULL CHECK (max_attempts > 0),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    last_error VARCHAR(1000),
    locked_by VARCHAR(160),
    locked_until TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE job_executions (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES scheduled_jobs (id),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    result VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_scheduled_jobs_due ON scheduled_jobs (status, run_at);
CREATE INDEX idx_job_executions_job_id ON job_executions (job_id);
