package com.maksud.jobplatform.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.worker.executer.JobExecutor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerServiceImpl implements WorkerService{

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobPayloadRepository payloadRepository;
    private final JobExecutor jobExecutor;
    private final JobExecutionService jobExecutionService;

    @Override
    @Transactional
    public void processJob(String message) {

        JobCreatedEvent event;
        try {
            event = objectMapper.readValue(message, JobCreatedEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Kafka message", e);
        }

        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Job not found : " + event.jobId()));

        JobPayload payload = payloadRepository.findByJob(job)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payload not found for job : " + job.getJobId()));

        try {
            // Atomic Job Claim
            int claimed = jobRepository.claimJob(
                    job.getJobId(),
                    JobStatus.QUEUED,
                    JobStatus.PROCESSING,
                    LocalDateTime.now()
            );

            if(claimed == 0){
                return;
            }

            boolean executionClaimed =
                    jobExecutionService.claimExecution(
                            job.getJobId(),
                            event.eventId()
                    );

            if (!executionClaimed) {
                log.info(
                        "Duplicate Kafka event ignored. eventId={}, jobId={}",
                        event.eventId(),
                        job.getJobId()
                );
                return;
            }

            jobExecutor.execute(
                    job.getJobType(),
                    payload.getPayload()
            );

            jobExecutionService.markCompleted(event.eventId());

            jobRepository.updateStatus(
                    job.getJobId(),
                    JobStatus.COMPLETED,
                    LocalDateTime.now()
            );
        } catch (Exception e) {

            jobExecutionService.markFailed(
                    event.eventId(),
                    e.getMessage()
            );

            jobRepository.updateExecutionResult(
                    job.getJobId(),
                    JobStatus.RETRYING,
                    job.getRetryCount() + 1,
                    LocalDateTime.now()
            );
            log.error("Failed processing job {}", job.getJobId(), e);
        }
    }
}
