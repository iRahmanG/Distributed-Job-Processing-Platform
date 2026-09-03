package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.entity.enums.JobStatus;

public interface JobLifecycleService {

    boolean claimQueuedJob(String jobId);

    void markRetry(String jobId);

    void completeJob(String jobId);
}
