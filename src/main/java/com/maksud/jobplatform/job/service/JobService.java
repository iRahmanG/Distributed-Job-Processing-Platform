package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.dto.CreateJobRequest;
import com.maksud.jobplatform.job.dto.CreateJobResponse;
import com.maksud.jobplatform.job.repository.JobRepository;

public interface JobService{
    CreateJobResponse createJob(CreateJobRequest request);
}
