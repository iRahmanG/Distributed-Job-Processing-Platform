package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.exception.InvalidJobStatusTransitionException;
import com.maksud.jobplatform.job.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobLifecycleServiceImpl implements JobLifecycleService {

    private final JobRepository jobRepository;

    @Value("${job-platform.retry.delay-seconds}")
    private long retryDelaySeconds;

    @Override
    @Transactional
    public boolean claimQueuedJob(String jobId) {

        int updated = jobRepository.claimJob(
                jobId,
                JobStatus.QUEUED,
                JobStatus.PROCESSING,
                LocalDateTime.now()
        );

        return updated == 1;
    }

    @Override
    @Transactional
    public void markRetry(String jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job not found : " + jobId
                        )
                );

        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new InvalidJobStatusTransitionException(
                    "Invalid job status transition: "
                            + job.getStatus()
                            + " -> "
                            + JobStatus.RETRYING
            );
        }

        int retryCount = job.getRetryCount() + 1;

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nextRetryAt =
                now.plusSeconds(retryDelaySeconds);

        int updated = jobRepository.markJobForRetry(
                jobId,
                JobStatus.PROCESSING,
                JobStatus.RETRYING,
                retryCount,
                nextRetryAt,
                now
        );

        if (updated != 1) {
            throw new InvalidJobStatusTransitionException(
                    "Job could not be moved to RETRYING: "
                            + jobId
            );
        }
    }

    @Override
    @Transactional
    public void completeJob(String jobId) {

        int updated = jobRepository.completeJob(
                jobId,
                JobStatus.PROCESSING,
                JobStatus.COMPLETED,
                LocalDateTime.now()
        );

        if (updated != 1) {
            throw new InvalidJobStatusTransitionException(
                    "Unable to transition job to COMPLETED: " + jobId
            );
        }
    }

    @Override
    @Transactional
    public boolean requeueRetryingJob(String jobId, LocalDateTime now) {

        int updated = jobRepository.requeueRetryingJob(
                jobId,
                JobStatus.RETRYING,
                JobStatus.QUEUED,
                now,
                now
        );

        return updated == 1;
    }
}