package com.maksud.jobplatform.worker.scheduler;

import com.maksud.jobplatform.deadletter.service.DeadLetterService;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.job.service.JobLifecycleService;
import com.maksud.jobplatform.outbox.service.OutboxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final JobRepository jobRepository;
    private final JobPayloadRepository jobPayloadRepository;
    private final DeadLetterService deadLetterService;
    private final OutboxService outboxService;
    private final JobLifecycleService jobLifecycleService;

    @Value("${job-platform.retry.max-attempts}")
    private int maxAttempts;
    @Scheduled(fixedDelayString = "${job-platform.retry.interval-ms}")
    @Transactional
    public void retryFailedJob() {

        LocalDateTime now = LocalDateTime.now();

        List<Job> jobs =
                jobRepository
                        .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                                JobStatus.RETRYING,
                                now
                        );

        for (Job job : jobs) {

            if (job.getRetryCount() >= maxAttempts) {

                JobPayload payload =
                        jobPayloadRepository.findByJob(job)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Payload not found for job "
                                                        + job.getJobId()
                                        )
                                );

                deadLetterService.moveToDeadLetter(
                        job,
                        payload.getPayload(),
                        new RuntimeException(
                                "Maximum retry attempts exceeded"
                        )
                );

                log.warn(
                        "Job {} moved to DLQ after {} attempts",
                        job.getJobId(),
                        job.getRetryCount()
                );

                continue;
            }

            boolean requeued =
                    jobLifecycleService.requeueRetryingJob(
                            job.getJobId(),
                            now
                    );

            if (!requeued) {
                continue;
            }

            outboxService.createJobCreatedEvent(job);

            log.info(
                    "Job {} requeued for retry. attempt={}",
                    job.getJobId(),
                    job.getRetryCount() + 1
            );
        }
    }
}