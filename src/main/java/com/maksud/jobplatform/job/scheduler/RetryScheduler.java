package com.maksud.jobplatform.job.scheduler;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.enums.EventType;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import com.maksud.jobplatform.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private static final int MAX_RETRY = 3;
    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;

    @Scheduled(fixedDelayString = "${job-platform.retry.interval-ms}")
    @Transactional
    public void retryFailedJob() {

        List<Job> jobs = jobRepository.findRetryableJobs(
                JobStatus.FAILED,
                MAX_RETRY
        );

        for(Job job: jobs) {
            job.setStatus(JobStatus.QUEUED);
            job.setRetryCount(job.getRetryCount() + 1);
            job.setUpdatedAt(LocalDateTime.now());

            jobRepository.save(job);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UlidCreator.getUlid().toString())
                    .jobId(job.getJobId())
                    .eventType(EventType.JOB_CREATED)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);

            log.info("Job {} scheduled for retry {}", job.getJobId(), job.getRetryCount());
        }

    }
}
