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
    public void transition(String jobId, JobStatus targetStatus) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job not found : " + jobId
                        )
                );

        JobStatus currentStatus = job.getStatus();

        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidJobStatusTransitionException(
                    "Invalid job status transition: "
                            + currentStatus
                            + " -> "
                            + targetStatus
            );
        }

        job.setStatus(targetStatus);
        job.setUpdatedAt(LocalDateTime.now());

        jobRepository.save(job);
    }

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

        int updated = jobRepository.updateRetryState(
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

    private boolean isValidTransition(
            JobStatus currentStatus,
            JobStatus targetStatus
    ) {

        return switch (currentStatus) {

            case CREATED ->
                    targetStatus == JobStatus.QUEUED;

            case QUEUED ->
                    targetStatus == JobStatus.PROCESSING;

            case PROCESSING ->
                    targetStatus == JobStatus.COMPLETED
                            || targetStatus == JobStatus.FAILED
                            || targetStatus == JobStatus.RETRYING;

            case RETRYING ->
                    targetStatus == JobStatus.QUEUED
                            || targetStatus == JobStatus.DEAD_LETTER;

            case FAILED ->
                    targetStatus == JobStatus.DEAD_LETTER;

            case COMPLETED, DEAD_LETTER -> false;
        };
    }
}