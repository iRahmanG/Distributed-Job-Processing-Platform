package com.maksud.jobplatform.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeartbeatManager {

    private final TaskScheduler taskScheduler;
    private final JobExecutionService jobExecutionService;

    private final Map<String, ScheduledFuture<?>> activeHeartbeats =
            new ConcurrentHashMap<>();

    public void startHeartbeat(String eventId) {

        ScheduledFuture<?> heartbeat = taskScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(eventId),
                Instant.now().plusSeconds(5),
                Duration.ofSeconds(5)
        );

        ScheduledFuture<?> existing =
                activeHeartbeats.putIfAbsent(eventId, heartbeat);

        if (existing != null) {
            heartbeat.cancel(false);
        }

        log.debug(
                "Heartbeat started. eventId={}",
                eventId
        );
    }

    public void stopHeartbeat(String eventId) {

        ScheduledFuture<?> heartbeat =
                activeHeartbeats.remove(eventId);

        if (heartbeat != null) {
            heartbeat.cancel(false);

            log.debug(
                    "Heartbeat stopped. eventId={}",
                    eventId
            );
        }
    }

    private void sendHeartbeat(String eventId) {

        try {

            boolean updated =
                    jobExecutionService.updateHeartbeat(eventId);

            if (!updated) {
                stopHeartbeat(eventId);
            }

        } catch (Exception e) {

            log.warn(
                    "Failed to update heartbeat. eventId={}",
                    eventId,
                    e
            );
        }
    }
}