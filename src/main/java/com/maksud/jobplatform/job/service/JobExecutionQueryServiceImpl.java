package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.dto.JobExecutionDetailsResponse;
import com.maksud.jobplatform.job.dto.JobExecutionResponse;
import com.maksud.jobplatform.job.entity.JobExecution;
import com.maksud.jobplatform.job.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobExecutionQueryServiceImpl implements JobExecutionQueryService {

    private final JobExecutionRepository jobExecutionRepository;

    @Override
    public List<JobExecutionResponse> getExecutions(String jobId) {

        return jobExecutionRepository
                .findByJobIdOrderByStartedAtDesc(jobId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public JobExecutionDetailsResponse getExecution(
            String jobId,
            String executionId
    ) {

        JobExecution execution = jobExecutionRepository
                .findByIdAndJobId(executionId, jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Execution not found: " + executionId
                        )
                );

        return toDetailsResponse(execution);
    }

    private JobExecutionResponse toResponse(JobExecution execution) {

        return new JobExecutionResponse(
                execution.getId(),
                execution.getEventId(),
                execution.getStatus(),
                execution.getWorkerId(),
                execution.getStartedAt(),
                execution.getCompletedAt()
        );
    }

    private JobExecutionDetailsResponse toDetailsResponse(
            JobExecution execution
    ) {

        return new JobExecutionDetailsResponse(
                execution.getId(),
                execution.getJobId(),
                execution.getEventId(),
                execution.getStatus(),
                execution.getWorkerId(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getErrorMessage()
        );
    }
}