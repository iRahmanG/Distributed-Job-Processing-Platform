package com.maksud.jobplatform.deadletter.dto;

import java.time.LocalDateTime;

public record DeadLetterDetailsResponse(
        String jobId,
        LocalDateTime failedAt,
        String reason,
        int retryCount,
        String stackTrace
) {
}
