package com.maksud.jobplatform.job.service;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.exception.InvalidJobStatusTransitionException;
import com.maksud.jobplatform.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobLifecycleServiceImplTest {
    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobLifecycleServiceImpl jobLifecycleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                jobLifecycleService,
                "retryDelaySeconds",
                30L
        );
    }

    @Test
    void shouldClaimQueuedJobSuccessfully(){
        when(jobRepository.claimJob(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);
        boolean result = jobLifecycleService.claimQueuedJob("job-123");
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseJobWasAlreadyClaimed() {
        when(jobRepository.claimJob(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0);

        boolean result = jobLifecycleService.claimQueuedJob("job-123");
        assertFalse(result);
    }

    @Test
    void shouldMarkProcessingJobForRetry() {
        Job job = Job.builder()
                .jobId("job-123")
                .status(JobStatus.PROCESSING)
                .retryCount(1)
                .build();

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobRepository.markJobForRetry(
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any()
        )).thenReturn(1);

        jobLifecycleService.markRetry("job-123");

        verify(jobRepository).markJobForRetry(
                eq("job-123"),
                eq(JobStatus.PROCESSING),
                eq(JobStatus.RETRYING),
                eq(2),
                any(),
                any()
        );
    }

    @Test
    void shouldThrowExceptionWhenMarkingNonProcessingJobForRetry(){
        Job job = Job.builder()
                .jobId("job-123")
                .status(JobStatus.COMPLETED)
                .retryCount(1)
                .build();

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        assertThrows(
                InvalidJobStatusTransitionException.class,
                () -> jobLifecycleService.markRetry("job-123")
        );

        verify(jobRepository, never()).markJobForRetry(
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any()
        );
    }

    @Test
    void shouldThrowExceptionWhenJobDoesNOtExist() {
        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> jobLifecycleService.markRetry("job-123")
        );

        verify(jobRepository, never()).markJobForRetry(
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any()
        );
    }

    @Test
    void shouldThrowExceptionWhenRetryUpdateFails() {

        Job job = Job.builder()
                .jobId("job-123")
                .status(JobStatus.PROCESSING)
                .retryCount(1)
                .build();

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobRepository.markJobForRetry(
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any()
        )).thenReturn(0);

        assertThrows(
                InvalidJobStatusTransitionException.class,
                () -> jobLifecycleService.markRetry("job-123")
        );
    }

    @Test
    void shouldCompleteProcessingJob() {
        when(jobRepository.completeJob(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);

        jobLifecycleService.completeJob("job-123");

        verify(jobRepository).completeJob(
                eq("job-123"),
                eq(JobStatus.PROCESSING),
                eq(JobStatus.COMPLETED),
                any()
        );
    }

    @Test
    void shouldThrowExceptionWhenCompletionUpdateFails() {

        when(jobRepository.completeJob(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0);

        assertThrows(
                InvalidJobStatusTransitionException.class,
                () -> jobLifecycleService.completeJob("job-123")
        );
    }

    @Test
    void shouldRequeueRetryingJobSuccessfully() {

        LocalDateTime now = LocalDateTime.now();
        when(jobRepository.requeueRetryingJob(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);

        boolean result =
                jobLifecycleService.requeueRetryingJob(
                        "job-123",
                        now
                );

        assertTrue(result);

        verify(jobRepository).requeueRetryingJob(
                eq("job-123"),
                eq(JobStatus.RETRYING),
                eq(JobStatus.QUEUED),
                eq(now),
                eq(now)
        );
    }

    @Test
    void shouldReturnFalseWhenRequeueFails() {

        LocalDateTime now = LocalDateTime.now();

        when(jobRepository.requeueRetryingJob(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0);

        boolean result =
                jobLifecycleService.requeueRetryingJob(
                        "job-123",
                        now
                );
        assertFalse(result);
    }
}
