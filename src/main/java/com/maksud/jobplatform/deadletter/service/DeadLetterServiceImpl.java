package com.maksud.jobplatform.deadletter.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.deadletter.dto.DeadLetterDetailsResponse;
import com.maksud.jobplatform.deadletter.dto.DeadLetterResponse;
import com.maksud.jobplatform.deadletter.entity.DeadLetterJob;
import com.maksud.jobplatform.deadletter.repository.DeadLetterRepository;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.service.OutboxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterServiceImpl implements DeadLetterService{

    private final DeadLetterRepository deadLetterRepository;
    private final JobRepository jobRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public void moveToDeadLetter(
            Job job,
            String payload,
            Exception exception
    ) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));

        LocalDateTime now = LocalDateTime.now();

        DeadLetterJob dlq = new DeadLetterJob();

        dlq.setId(UlidCreator.getUlid().toString());
        dlq.setJobId(job.getJobId());
        dlq.setJobType(job.getJobType());
        dlq.setPayload(payload);
        dlq.setReason(exception.getMessage());
        dlq.setRetryCount(job.getRetryCount());
        dlq.setFailedAt(now);
        dlq.setStackTrace(sw.toString());

        deadLetterRepository.save(dlq);

        int updated = jobRepository.markDeadLetter(
                job.getJobId(),
                JobStatus.RETRYING,
                JobStatus.DEAD_LETTER,
                now
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Unable to move job to DEAD_LETTER: "
                            + job.getJobId()
            );
        }

        log.error(
                "Job {} moved to DLQ after {} retries",
                job.getJobId(),
                job.getRetryCount()
        );
    }

    @Override
    public List<DeadLetterResponse> getAllJobs() {

        return deadLetterRepository.findAllByOrderByFailedAtDesc()
                .stream()
                .map(dlq -> new DeadLetterResponse(
                        dlq.getJobId(),
                        dlq.getFailedAt(),
                        dlq.getReason(),
                        dlq.getRetryCount()
                ))
                .toList();
    }

    @Override
    public DeadLetterDetailsResponse getJob(String jobId) {

        DeadLetterJob job = deadLetterRepository.findByJobId(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException("DLQ Job not found : " + jobId));

        return new DeadLetterDetailsResponse(
                job.getJobId(),
                job.getFailedAt(),
                job.getReason(),
                job.getRetryCount(),
                job.getStackTrace()
        );
    }

    @Override
    @Transactional
    public void replayJob(String jobId) {

        DeadLetterJob dlq = deadLetterRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DLQ Jobnot found : " + jobId
                ));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job not found : " + jobId
                ));

        job.setStatus(JobStatus.QUEUED);
        job.setRetryCount(0);
        job.setUpdatedAt(LocalDateTime.now());

        jobRepository.save(job);

        outboxService.createJobCreatedEvent(job);

        deadLetterRepository.delete(dlq);

        log.info("Replay scheduled for job {}", jobId);
    }

    @Override
    public void deleteJob(String jobId) {

        DeadLetterJob job = deadLetterRepository.findByJobId(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException("DLQ Job not found : " + jobId));

        deadLetterRepository.delete(job);

        log.info("Deleted DLQ job {}", jobId);
    }

}
