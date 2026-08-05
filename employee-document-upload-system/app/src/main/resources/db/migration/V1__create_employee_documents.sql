CREATE TABLE employee_documents (
    id UUID PRIMARY KEY,
    owner_user_id VARCHAR(160) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    content_length_bytes BIGINT NOT NULL,
    storage_bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by VARCHAR(160),
    review_note VARCHAR(1000)
);

CREATE INDEX idx_employee_documents_owner_created
    ON employee_documents (owner_user_id, created_at DESC);

CREATE INDEX idx_employee_documents_status_created
    ON employee_documents (status, created_at DESC);
