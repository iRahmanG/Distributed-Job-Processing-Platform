package com.maksud.jobplatform.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.worker.executer.JobExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService{

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobPayloadRepository payloadRepository;
    private final JobExecutor jobExecutor;

    @Override
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
            job.setStatus(JobStatus.PROCESSING);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            jobExecutor.execute(
                    job.getJobType(),
                    payload.getPayload()
            );

            job.setStatus(JobStatus.COMPLETED);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            throw e;
        }

        jobRepository.save(job);
    }
}
