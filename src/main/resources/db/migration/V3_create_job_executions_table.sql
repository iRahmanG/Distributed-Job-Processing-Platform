CREATE TABLE job_executions (
    id VARCHAR(26) PRIMARY KEY,
    job_id VARCHAR(26) NOT NULL,
    event_id VARCHAR(26) NOT NULL,
    status VARCHAR(20) NOT NULL,
    worker_id VARCHAR(100),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,

    CONSTRAINT uk_job_execution_event
        UNIQUE (event_id)
);


CREATE INDEX idx_job_executions_job_id
ON job_executions(job_id);