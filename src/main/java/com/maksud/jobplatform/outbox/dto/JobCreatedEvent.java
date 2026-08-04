package com.maksud.jobplatform.outbox.dto;

public record JobCreatedEvent(
        String eventId,
        String jobId,
        String eventType
) {}