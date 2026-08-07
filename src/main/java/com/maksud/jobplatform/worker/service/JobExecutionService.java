package com.maksud.jobplatform.worker.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.job.entity.JobExecution;
import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;
import com.maksud.jobplatform.job.repository.JobExecutionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimExecution(String jobId, String eventId) {

        JobExecution execution = JobExecution.builder()
                .id(UlidCreator.getUlid().toString())
                .jobId(jobId)
                .eventId(eventId)
                .status(ExecutionStatus.PROCESSING)
                .workerId(getWorkerId())
                .startedAt(LocalDateTime.now())
                .build();

        try {
            jobExecutionRepository.saveAndFlush(execution);
            return true;

        } catch (DataIntegrityViolationException e) {
            // event_id already exists.
            return false;
        }
    }

    @Transactional
    public void markCompleted(String eventId) {
        jobExecutionRepository.updateStatus(
                eventId,
                ExecutionStatus.COMPLETED,
                LocalDateTime.now()
        );
    }

    @Transactional
    public void markFailed(String eventId, String errorMessage) {
        jobExecutionRepository.updateFailure(
                eventId,
                ExecutionStatus.FAILED,
                errorMessage,
                LocalDateTime.now()
        );
    }

    private String getWorkerId() {
        return System.getProperty("user.name") + "-"
                + System.getenv().getOrDefault("HOSTNAME", "local");
    }
}
