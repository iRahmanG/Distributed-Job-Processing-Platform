package com.maksud.jobplatform.job.dto;

import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;

import java.time.LocalDateTime;

public record JobExecutionDetailsResponse(
        String executionId,
        String jobId,
        String eventId,
        ExecutionStatus status,
        String workerId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
}
