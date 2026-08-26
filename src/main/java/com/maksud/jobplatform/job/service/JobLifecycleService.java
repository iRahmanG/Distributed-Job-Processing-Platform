package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.entity.enums.JobStatus;

public interface JobLifecycleService {

    void transition(String jobId, JobStatus targetStatus);

    boolean claimQueuedJob(String jobId);

    void markRetry(String jobId);
}
