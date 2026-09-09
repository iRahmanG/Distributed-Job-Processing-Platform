ALTER TABLE job_executions
    ADD COLUMN last_heartbeat_at TIMESTAMP;

CREATE INDEX idx_job_executions_heartbeat
    ON job_executions(last_heartbeat_at);