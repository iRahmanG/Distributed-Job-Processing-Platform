package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.JobExecution;
import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobExecutionRepository extends JpaRepository<JobExecution, String> {

    Optional<JobExecution> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<JobExecution> findByJobIdOrderByStartedAtDesc(String jobId);

    Optional<JobExecution> findByIdAndJobId(
            String executionId,
            String jobId
    );

    @Modifying
    @Query(value = """
        INSERT INTO job_executions
        (
            id,
            job_id,
            event_id,
            status,
            worker_id,
            started_at,
            last_heartbeat_at
        )
        VALUES
        (
            :id,
            :jobId,
            :eventId,
            :status,
            :workerId,
            :startedAt,
            :startedAt
        )
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfNotExists(
            @Param("id") String id,
            @Param("jobId") String jobId,
            @Param("eventId") String eventId,
            @Param("status") String status,
            @Param("workerId") String workerId,
            @Param("startedAt") LocalDateTime startedAt
    );

    @Modifying
    @Query("""
        UPDATE JobExecution e
        SET e.lastHeartbeatAt = :heartbeatAt
        WHERE e.eventId = :eventId
          AND e.status = :processingStatus
        """)
    int updateHeartbeat(
            @Param("eventId") String eventId,
            @Param("processingStatus") ExecutionStatus processingStatus,
            @Param("heartbeatAt") LocalDateTime heartbeatAt
    );

    @Modifying
    @Query("""
            UPDATE JobExecution e
            SET e.status = :status,
                e.completedAt = :completedAt
            WHERE e.eventId = :eventId
            """)
    int updateStatus(
            @Param("eventId") String eventId,
            @Param("status") ExecutionStatus status,
            @Param("completedAt") LocalDateTime completedAt
    );

    @Modifying
    @Query("""
            UPDATE JobExecution e
            SET e.status = :status,
                e.errorMessage = :errorMessage,
                e.completedAt = :completedAt
            WHERE e.eventId = :eventId
            """)
    int updateFailure(
            @Param("eventId") String eventId,
            @Param("status") ExecutionStatus status,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") LocalDateTime completedAt
    );
}