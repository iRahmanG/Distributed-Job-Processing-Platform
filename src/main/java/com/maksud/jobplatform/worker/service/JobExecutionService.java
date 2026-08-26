package com.maksud.jobplatform.worker.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;
import com.maksud.jobplatform.job.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;

    @Transactional
    public boolean claimExecution(String jobId, String eventId) {

        int inserted = jobExecutionRepository.insertIfNotExists(
                UlidCreator.getUlid().toString(),
                jobId,
                eventId,
                ExecutionStatus.PROCESSING.name(),
                getWorkerId(),
                LocalDateTime.now()
        );

        return inserted == 1;
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
    public void markFailed(
            String eventId,
            String errorMessage
    ) {

        jobExecutionRepository.updateFailure(
                eventId,
                ExecutionStatus.FAILED,
                errorMessage,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public boolean executionExists(String eventId) {
        return jobExecutionRepository.existsByEventId(eventId);
    }

    private String getWorkerId() {

        return System.getProperty("user.name")
                + "-"
                + System.getenv().getOrDefault(
                "HOSTNAME",
                "local"
        );
    }
}