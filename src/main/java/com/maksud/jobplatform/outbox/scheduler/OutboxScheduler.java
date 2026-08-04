package com.maksud.jobplatform.outbox.scheduler;

import com.maksud.jobplatform.outbox.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxPublisherService outboxPublisherService;

    @Scheduled(fixedDelayString = "${job-platform.outbox.publish-interval-ms}")
    public void publishPendingEvents(){
        outboxPublisherService.publishPendingEvent();
    }

}
