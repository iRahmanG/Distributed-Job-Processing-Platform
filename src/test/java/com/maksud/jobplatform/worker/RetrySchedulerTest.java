package com.maksud.jobplatform.worker;

import com.maksud.jobplatform.deadletter.service.DeadLetterService;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.job.service.JobLifecycleService;
import com.maksud.jobplatform.outbox.service.OutboxService;
import com.maksud.jobplatform.worker.scheduler.RetryScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RetrySchedulerTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobPayloadRepository jobPayloadRepository;
    @Mock
    private DeadLetterService deadLetterService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private JobLifecycleService jobLifecycleService;

    @InjectMocks
    private RetryScheduler retryScheduler;

    @BeforeEach()
    void setUp() {
        ReflectionTestUtils.setField(
                retryScheduler,
                "maxAttempts",
                3
        );
    }

    @Test
    void shouldRequeueJobAndCreateOutboxEventWhenRetryIsAvailable() {
        LocalDateTime now = LocalDateTime.now();

        Job job = Job.builder()
                .jobId("job-123")
                .jobType("EMAIL")
                .status(JobStatus.RETRYING)
                .retryCount(1)
                .nextRetryAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(20))
                .build();

        when(jobRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        eq(JobStatus.RETRYING),
                        any(LocalDateTime.class)
                )).thenReturn(List.of(job));
        when(jobLifecycleService.requeueRetryingJob(
                eq("job-123"),
                any(LocalDateTime.class)
        )).thenReturn(true);

        retryScheduler.retryFailedJob();

        verify(outboxService).createJobCreatedEvent(job);
        verify(deadLetterService, never())
                .moveToDeadLetter(any(),anyString(),any());
    }
    @Test
    void shouldNotCreateOutboxEventWhenRequeueFails() {

        LocalDateTime now = LocalDateTime.now();
        Job job = Job.builder()
                .jobId("job-123")
                .jobType("EMAIL")
                .status(JobStatus.RETRYING)
                .retryCount(1)
                .nextRetryAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(20))
                .build();

        when(jobRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        eq(JobStatus.RETRYING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(job));

        when(jobLifecycleService.requeueRetryingJob(
                eq("job-123"),
                any(LocalDateTime.class)
        )).thenReturn(false);

        retryScheduler.retryFailedJob();

        verify(jobLifecycleService).requeueRetryingJob(
                eq("job-123"),
                any(LocalDateTime.class)
        );

        verify(outboxService, never())
                .createJobCreatedEvent(any(Job.class));
        verify(deadLetterService, never())
                .moveToDeadLetter(any(), anyString(), any());
    }
    @Test
    void shouldMoveJobToDeadLetterWhenMaxAttemptsExceeded() {

        LocalDateTime now = LocalDateTime.now();

        Job job = Job.builder()
                .jobId("job-123")
                .jobType("EMAIL")
                .status(JobStatus.RETRYING)
                .retryCount(3)
                .nextRetryAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(20))
                .build();

        JobPayload payload = JobPayload.builder()
                .job(job)
                .payload("{\"email\":\"test@example.com\"}")
                .build();

        when(jobRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        eq(JobStatus.RETRYING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        retryScheduler.retryFailedJob();

        verify(jobPayloadRepository).findByJob(job);

        verify(deadLetterService).moveToDeadLetter(
                eq(job),
                eq(payload.getPayload()),
                any(RuntimeException.class)
        );

        verify(jobLifecycleService, never())
                .requeueRetryingJob(anyString(), any(LocalDateTime.class));

        verify(outboxService, never())
                .createJobCreatedEvent(any(Job.class));
    }
    @Test
    void shouldThrowExceptionWhenPayloadIsMissingForDeadLetterJob() {
        LocalDateTime now = LocalDateTime.now();

        Job job = Job.builder()
                .jobId("job-123")
                .jobType("EMAIL")
                .status(JobStatus.RETRYING)
                .retryCount(3)
                .nextRetryAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(20))
                .build();

        when(jobRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        eq(JobStatus.RETRYING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> retryScheduler.retryFailedJob()
        );

        verify(deadLetterService, never())
                .moveToDeadLetter(any(), anyString(), any());
        verify(jobLifecycleService, never())
                .requeueRetryingJob(anyString(), any(LocalDateTime.class));
    }
    @Test
    void shouldProcessMultipleRetryableJobsIndependently() {

        LocalDateTime now = LocalDateTime.now();

        Job job1 = Job.builder()
                .jobId("job-1")
                .jobType("EMAIL")
                .status(JobStatus.RETRYING)
                .retryCount(1)
                .nextRetryAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(20))
                .build();

        Job job2 = Job.builder()
                .jobId("job-2")
                .jobType("REPORT")
                .status(JobStatus.RETRYING)
                .retryCount(1)
                .nextRetryAt(now.minusSeconds(5))
                .updatedAt(now.minusSeconds(15))
                .build();

        when(jobRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        eq(JobStatus.RETRYING),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(job1, job2));

        when(jobLifecycleService.requeueRetryingJob(
                eq("job-1"),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(jobLifecycleService.requeueRetryingJob(
                eq("job-2"),
                any(LocalDateTime.class)
        )).thenReturn(false);

        retryScheduler.retryFailedJob();

        verify(jobLifecycleService).requeueRetryingJob(
                eq("job-1"),
                any(LocalDateTime.class)
        );

        verify(jobLifecycleService).requeueRetryingJob(
                eq("job-2"),
                any(LocalDateTime.class)
        );

        verify(outboxService).createJobCreatedEvent(job1);

        verify(outboxService, never())
                .createJobCreatedEvent(job2);

        verifyNoInteractions(deadLetterService);
    }
}
