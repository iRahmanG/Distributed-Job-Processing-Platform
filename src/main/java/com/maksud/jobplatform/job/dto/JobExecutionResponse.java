package com.maksud.jobplatform.job.dto;

import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;

import java.time.LocalDateTime;

public record JobExecutionResponse(
        String executionId,
        String eventId,
        ExecutionStatus status,
        String workerId,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
