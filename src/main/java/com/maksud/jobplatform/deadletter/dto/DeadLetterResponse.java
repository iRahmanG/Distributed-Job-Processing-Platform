package com.maksud.jobplatform.deadletter.dto;

import java.time.LocalDateTime;

public record DeadLetterResponse(
        String jobId,
        LocalDateTime failedAt,
        String reason,
        int retryCount
) {
}
