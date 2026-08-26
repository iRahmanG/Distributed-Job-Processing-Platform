package com.maksud.jobplatform.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import com.maksud.jobplatform.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private static final String TOPIC = "jobs.created";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;

    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                );

        for (OutboxEvent event : events) {

            try {

                Job job = jobRepository.findById(event.getJobId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Job not found: " + event.getJobId()
                                )
                        );

                /*
                 * CREATED → QUEUED
                 *
                 * The worker expects the job to already be QUEUED
                 * when the Kafka event is consumed.
                 */
                if (job.getStatus() != JobStatus.CREATED) {

                    log.warn(
                            "Skipping outbox event {} because job {} is in status {}",
                            event.getEventId(),
                            job.getJobId(),
                            job.getStatus()
                    );

                    continue;
                }

                job.setStatus(JobStatus.QUEUED);
                job.setUpdatedAt(LocalDateTime.now());

                /*
                 * Important:
                 * Force the UPDATE to be sent to PostgreSQL
                 * before publishing the Kafka message.
                 */
                jobRepository.saveAndFlush(job);

                JobCreatedEvent kafkaEvent = new JobCreatedEvent(
                        event.getEventId(),
                        event.getJobId(),
                        event.getEventType().name()
                );

                String message =
                        objectMapper.writeValueAsString(kafkaEvent);

                /*
                 * Kafka publish happens only after the job
                 * has been moved to QUEUED.
                 */
                kafkaTemplate
                        .send(
                                TOPIC,
                                event.getJobId(),
                                message
                        )
                        .get();

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());

                outboxRepository.save(event);

                log.info(
                        "Published outbox event {} for job {}",
                        event.getEventId(),
                        job.getJobId()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to publish event {}",
                        event.getEventId(),
                        e
                );

                throw new RuntimeException(e);
            }
        }
    }
}