package com.maksud.jobplatform.worker.kafka;

import com.maksud.jobplatform.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobConsumer {

    private final WorkerService workerService;

    @KafkaListener(
            topics = "jobs.created",
            groupId = "job-workers"
    )
    public void consume(String message){
        log.info("========== KAFKA MESSAGE RECEIVED ==========");
        log.info("Received Kafka message: {}", message);

        workerService.processJob(message);
    }
}
