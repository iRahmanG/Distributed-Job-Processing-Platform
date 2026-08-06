package com.maksud.jobplatform.outbox.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.enums.EventType;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import com.maksud.jobplatform.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void createJobCreatedEvent(Job job) {

        OutboxEvent event = OutboxEvent.builder()
                .eventId(UlidCreator.getUlid().toString())
                .jobId(job.getJobId())
                .eventType(EventType.JOB_CREATED)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(event);
    }
}
