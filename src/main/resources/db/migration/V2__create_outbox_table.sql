CREATE TABLE outbox_events (
    event_id VARCHAR(26) PRIMARY KEY,
    job_id VARCHAR(26) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_status_created_at
ON outbox_events(status, created_at);