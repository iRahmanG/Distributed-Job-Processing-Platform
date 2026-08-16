package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.dto.JobExecutionDetailsResponse;
import com.maksud.jobplatform.job.dto.JobExecutionResponse;

import java.util.List;

public interface JobExecutionQueryService {

    List<JobExecutionResponse> getExecutions(String jobId);

    JobExecutionDetailsResponse getExecution(
            String jobId,
            String executionId
    );
}
