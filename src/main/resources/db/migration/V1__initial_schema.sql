-- ==========================================
-- V1__initial_schema.sql
-- Initial schema for Distributed Job Platform
-- ==========================================

CREATE TABLE jobs (

    job_id VARCHAR(26) PRIMARY KEY,

    job_type VARCHAR(50) NOT NULL,

    status VARCHAR(20) NOT NULL
        CHECK (
            status IN (
                'CREATED',
                'QUEUED',
                'PROCESSING',
                'COMPLETED',
                'FAILED',
                'RETRYING'
            )
        ),

    priority VARCHAR(20) NOT NULL
        CHECK (
            priority IN (
                'LOW',
                'NORMAL',
                'HIGH',
                'CRITICAL'
            )
        ),

    retry_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--------------------------------------------------------

CREATE TABLE job_payloads (

    payload_id BIGSERIAL PRIMARY KEY,

    job_id VARCHAR(26) NOT NULL UNIQUE,

    payload JSONB NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_payload
        FOREIGN KEY (job_id)
        REFERENCES jobs(job_id)
);

--------------------------------------------------------

CREATE TABLE outbox (

    event_id BIGSERIAL PRIMARY KEY,

    job_id VARCHAR(26) NOT NULL,

    event_type VARCHAR(50) NOT NULL,

    payload JSONB NOT NULL,

    published BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    published_at TIMESTAMP,

    CONSTRAINT fk_outbox_job
        FOREIGN KEY (job_id)
        REFERENCES jobs(job_id)
);

--------------------------------------------------------
-- Indexes
--------------------------------------------------------

CREATE INDEX idx_jobs_status
ON jobs(status);

CREATE INDEX idx_jobs_created_at
ON jobs(created_at);

CREATE INDEX idx_outbox_published
ON outbox(published);

ALTER TABLE jobs
ADD COLUMN next_retry_at TIMESTAMP;