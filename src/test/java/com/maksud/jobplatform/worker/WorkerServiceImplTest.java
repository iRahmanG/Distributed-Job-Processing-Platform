package com.maksud.jobplatform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.job.service.JobLifecycleService;
import com.maksud.jobplatform.outbox.dto.JobCreatedEvent;
import com.maksud.jobplatform.worker.executer.JobExecutor;
import com.maksud.jobplatform.worker.service.JobExecutionService;
import com.maksud.jobplatform.worker.service.WorkerServiceImpl;
import netscape.javascript.JSObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkerServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobPayloadRepository jobPayloadRepository;
    @Mock
    private JobExecutor jobExecutor;
    @Mock
    private JobExecutionService jobExecutionService;
    @Mock
    private JobLifecycleService jobLifecycleService;

    @InjectMocks
    private WorkerServiceImpl workerService;

    @Test
    void shouldIgnoreDuplicateKafkaEvent() throws Exception {
        JobCreatedEvent event = mock(JobCreatedEvent.class);

        when(event.eventId())
                .thenReturn("event-123");

        when(event.jobId())
                .thenReturn("job-123");
        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(true);
        workerService.processJob("test-message");
        verify(jobExecutionService)
                .executionExists("event-123");

        verifyNoInteractions(
                jobRepository,
                jobPayloadRepository,
                jobExecutor,
                jobLifecycleService
        );

    }

    @Test
    void shouldThrowExceptionWhenJobDoesNotExist() throws Exception {
        JobCreatedEvent event = mock(JobCreatedEvent.class);
        when(event.eventId())
                .thenReturn("event-123");
        when(event.jobId())
                .thenReturn("job-123");
        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> workerService.processJob("test-message")
        );

        verify(jobExecutionService)
                .executionExists("event-123");

        verify(jobRepository)
                .findById("job-123");

        verifyNoInteractions(
                jobPayloadRepository,
                jobExecutor,
                jobLifecycleService
        );
    }

    @Test
    void shouldThrowExceptionWhenPayloadDoesNotExist() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);

        when(event.eventId()).thenReturn("event-123");
        when(event.jobId()).thenReturn("job-123");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> workerService.processJob("test-message")
        );

        verify(jobRepository).findById("job-123");
        verify(jobPayloadRepository).findByJob(job);

        verifyNoInteractions(
                jobExecutor,
                jobLifecycleService
        );
    }

    @Test
    void shouldReturnWhenJobCannotBeClaimed() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-123");
        when(event.jobId()).thenReturn("job-123");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-123");

        when(jobLifecycleService.claimQueuedJob("job-123"))
                .thenReturn(false);

        workerService.processJob("test-message");

        verify(jobLifecycleService)
                .claimQueuedJob("job-123");

        verify(jobExecutionService, never())
                .claimExecution(anyString(), anyString());

        verify(jobExecutor, never())
                .execute(anyString(), anyString());

        verify(jobLifecycleService, never())
                .completeJob(anyString());
    }

    @Test
    void shouldThrowExceptionWhenJobMissingAfterClaim() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-123");
        when(event.jobId()).thenReturn("job-123");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.empty());

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-123");

        when(jobLifecycleService.claimQueuedJob("job-123"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerService.processJob("test-message")
        );

        verify(jobLifecycleService)
                .claimQueuedJob("job-123");

        verify(jobRepository, times(2))
                .findById("job-123");

        verify(jobExecutionService, never())
                .claimExecution(anyString(), anyString());

        verify(jobExecutor, never())
                .execute(anyString(), anyString());
    }

    @Test
    void shouldReturnWhenExecutionCannotBeClaimed() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-123");
        when(event.jobId()).thenReturn("job-123");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-123");

        when(jobLifecycleService.claimQueuedJob("job-123"))
                .thenReturn(true);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobExecutionService.claimExecution(
                "job-123",
                "event-123"
        )).thenReturn(false);

        workerService.processJob("test-message");

        verify(jobExecutionService)
                .claimExecution("job-123", "event-123");

        verify(jobExecutor, never())
                .execute(anyString(), anyString());

        verify(jobExecutionService, never())
                .markCompleted(anyString());

        verify(jobExecutionService, never())
                .markFailed(anyString(), anyString());

        verify(jobLifecycleService, never())
                .completeJob(anyString());

        verify(jobLifecycleService, never())
                .markRetry(anyString());
    }

    @Test
    void shouldThrowExceptionWhenKafkaMessageIsInvalid() throws Exception {

        when(objectMapper.readValue(
                "invalid-message",
                JobCreatedEvent.class
        )).thenThrow(new RuntimeException("Invalid JSON"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> workerService.processJob("invalid-message")
        );

        assertEquals("Invalid Kafka message", exception.getMessage());

        verifyNoInteractions(
                jobExecutionService,
                jobRepository,
                jobPayloadRepository,
                jobExecutor,
                jobLifecycleService
        );
    }

    @Test
    void shouldMarkExecutionFailedAndScheduleRetryWhenExecutionFails() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-123");
        when(event.jobId()).thenReturn("job-123");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-123"))
                .thenReturn(false);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-123");
        when(job.getJobType()).thenReturn("EMAIL");

        when(payload.getPayload())
                .thenReturn("{\"to\":\"test@example.com\"}");

        when(jobLifecycleService.claimQueuedJob("job-123"))
                .thenReturn(true);

        when(jobRepository.findById("job-123"))
                .thenReturn(Optional.of(job));

        when(jobExecutionService.claimExecution(
                "job-123",
                "event-123"
        )).thenReturn(true);

        RuntimeException failure =
                new RuntimeException("Email service unavailable");

        doThrow(failure)
                .when(jobExecutor)
                .execute(
                        "EMAIL",
                        "{\"to\":\"test@example.com\"}"
                );

        workerService.processJob("test-message");

        verify(jobExecutor)
                .execute(
                        "EMAIL",
                        "{\"to\":\"test@example.com\"}"
                );

        verify(jobExecutionService)
                .markFailed(
                        "event-123",
                        "Email service unavailable"
                );

        verify(jobLifecycleService)
                .markRetry("job-123");

        verify(jobExecutionService, never())
                .markCompleted(anyString());

        verify(jobLifecycleService, never())
                .completeJob(anyString());
    }

    @Test
    void shouldMarkExecutionCompletedAndJobCompletedAfterSuccessfulExecution()
            throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-456");
        when(event.jobId()).thenReturn("job-456");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-456"))
                .thenReturn(false);

        when(jobRepository.findById("job-456"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-456");
        when(job.getJobType()).thenReturn("REPORT");

        when(payload.getPayload())
                .thenReturn("{\"reportId\":\"r-123\"}");

        when(jobLifecycleService.claimQueuedJob("job-456"))
                .thenReturn(true);

        when(jobRepository.findById("job-456"))
                .thenReturn(Optional.of(job));

        when(jobExecutionService.claimExecution(
                "job-456",
                "event-456"
        )).thenReturn(true);

        workerService.processJob("test-message");

        verify(jobExecutor)
                .execute(
                        "REPORT",
                        "{\"reportId\":\"r-123\"}"
                );

        verify(jobExecutionService)
                .markCompleted("event-456");

        verify(jobLifecycleService)
                .completeJob("job-456");

        verify(jobExecutionService, never())
                .markFailed(anyString(), anyString());

        verify(jobLifecycleService, never())
                .markRetry(anyString());
    }

    @Test
    void shouldNeverExecuteJobWhenExecutionClaimFails() throws Exception {

        JobCreatedEvent event = mock(JobCreatedEvent.class);
        Job job = mock(Job.class);
        JobPayload payload = mock(JobPayload.class);

        when(event.eventId()).thenReturn("event-race");
        when(event.jobId()).thenReturn("job-race");

        when(objectMapper.readValue(
                "test-message",
                JobCreatedEvent.class
        )).thenReturn(event);

        when(jobExecutionService.executionExists("event-race"))
                .thenReturn(false);

        when(jobRepository.findById("job-race"))
                .thenReturn(Optional.of(job));

        when(jobPayloadRepository.findByJob(job))
                .thenReturn(Optional.of(payload));

        when(job.getJobId()).thenReturn("job-race");

        when(jobLifecycleService.claimQueuedJob("job-race"))
                .thenReturn(true);

        when(jobRepository.findById("job-race"))
                .thenReturn(Optional.of(job));

        when(jobExecutionService.claimExecution(
                "job-race",
                "event-race"
        )).thenReturn(false);

        workerService.processJob("test-message");

        verify(jobExecutionService)
                .claimExecution("job-race", "event-race");

        verify(jobExecutor, never())
                .execute(anyString(), anyString());

        verify(jobExecutionService, never())
                .markCompleted(anyString());

        verify(jobExecutionService, never())
                .markFailed(anyString(), anyString());

        verify(jobLifecycleService, never())
                .completeJob(anyString());

        verify(jobLifecycleService, never())
                .markRetry(anyString());
    }
}
