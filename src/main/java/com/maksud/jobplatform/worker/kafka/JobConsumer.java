package com.maksud.jobplatform.worker.kafka;

import com.maksud.jobplatform.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobConsumer {

    private final WorkerService workerService;

    @KafkaListener(
            topics = "job.created",
            groupId = "job-workers"
    )
    public void consume(String message){

    }
}
