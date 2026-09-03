package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.entity.enums.JobStatus;

import java.time.LocalDateTime;

public interface JobLifecycleService {

    boolean claimQueuedJob(String jobId);

    void markRetry(String jobId);

    void completeJob(String jobId);

    boolean requeueRetryingJob(String jobId, LocalDateTime now);
}
