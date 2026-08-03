package com.maksud.jobplatform.job.entity.enums;

public enum JobStatus {
    CREATED,      // Accepted by API and stored in DB
    QUEUED,       // Outbox published, waiting for worker
    PROCESSING,   // Worker picked the job
    COMPLETED,    // Success
    FAILED,       // Permanent failure
    RETRYING      // Waiting for retry
}
