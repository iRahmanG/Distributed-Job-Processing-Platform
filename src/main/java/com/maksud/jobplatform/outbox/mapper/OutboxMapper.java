package com.maksud.jobplatform.outbox.mapper;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.enums.EventType;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OutboxMapper {

    public OutboxEvent toOutBoxEvent(
            Job job,
            LocalDateTime now,
            String eventId
    ) {
        return OutboxEvent.builder()
                .eventId(eventId)
                .jobId(job.getJobId())
                .eventType(EventType.JOB_CREATED)
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .build();
    }
}
