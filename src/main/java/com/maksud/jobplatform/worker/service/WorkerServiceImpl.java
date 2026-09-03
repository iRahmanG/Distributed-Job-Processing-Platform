package com.maksud.jobplatform.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.job.service.JobLifecycleService;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.worker.executer.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerServiceImpl implements WorkerService {

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobPayloadRepository payloadRepository;
    private final JobExecutor jobExecutor;
    private final JobExecutionService jobExecutionService;
    private final JobLifecycleService jobLifecycleService;

    @Override
    public void processJob(String message) {

        log.info("Received Kafka job event: {}", message);

        JobCreatedEvent event;

        try {
            event = objectMapper.readValue(
                    message,
                    JobCreatedEvent.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid Kafka message",
                    e
            );
        }

        // Ignore duplicate Kafka events
        if (jobExecutionService.executionExists(event.eventId())) {

            log.info(
                    "Duplicate Kafka event ignored. eventId={}, jobId={}",
                    event.eventId(),
                    event.jobId()
            );

            return;
        }

        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job not found : " + event.jobId()
                        )
                );

        JobPayload payload = payloadRepository.findByJob(job)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payload not found for job : "
                                        + job.getJobId()
                        )
                );

        // Try to claim the job
        boolean jobClaimed =
                jobLifecycleService.claimQueuedJob(
                        job.getJobId()
                );

        if (!jobClaimed) {

            log.info(
                    "Job could not be claimed. jobId={}, currentStatus={}",
                    job.getJobId(),
                    job.getStatus()
            );

            return;
        }

        // Reload after the bulk update
        Job processingJob = jobRepository.findById(event.jobId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job not found after claim : "
                                        + event.jobId()
                        )
                );

        boolean executionClaimed =
                jobExecutionService.claimExecution(
                        processingJob.getJobId(),
                        event.eventId()
                );

        if (!executionClaimed) {

            log.info(
                    "Execution already exists. eventId={}, jobId={}",
                    event.eventId(),
                    event.jobId()
            );

            return;
        }

        try {

            jobExecutor.execute(
                    processingJob.getJobType(),
                    payload.getPayload()
            );

            jobExecutionService.markCompleted(
                    event.eventId()
            );

            jobLifecycleService.completeJob(
                    processingJob.getJobId()
            );

            log.info(
                    "Job completed successfully. jobId={}, eventId={}",
                    processingJob.getJobId(),
                    event.eventId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed processing job {}",
                    processingJob.getJobId(),
                    e
            );

            jobExecutionService.markFailed(
                    event.eventId(),
                    e.getMessage()
            );

            jobLifecycleService.markRetry(
                    processingJob.getJobId()
            );

            log.info(
                    "Job scheduled for retry. jobId={}",
                    processingJob.getJobId()
            );
        }
    }
}