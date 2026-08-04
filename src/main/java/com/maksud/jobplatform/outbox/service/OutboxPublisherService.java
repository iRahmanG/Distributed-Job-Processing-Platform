package com.maksud.jobplatform.outbox.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import com.maksud.jobplatform.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private static final String TOPIC = "jobs.created";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishPendingEvent(){
        List<OutboxEvent> events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING
        );

        for(OutboxEvent event: events){
            JobCreatedEvent kafakEvent = new JobCreatedEvent(
              event.getEventId(),
              event.getJobId(),
              event.getEventType().name()
            );

            try {
                String message  = objectMapper.writeValueAsString(kafakEvent);
                kafkaTemplate
                        .send(TOPIC, event.getJobId(), message)
                        .get();

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());

                outboxRepository.save(event);
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }
}
