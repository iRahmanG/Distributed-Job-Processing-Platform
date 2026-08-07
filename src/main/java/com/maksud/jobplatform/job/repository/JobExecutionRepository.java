package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.JobExecution;
import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JobExecutionRepository extends JpaRepository<JobExecution, String> {

    Optional<JobExecution> findByEventId(String eventId);

    boolean existByEventId(String eventId);

    @Modifying
    @Query("""
        UPDATE JobExecution e
        SET e.status = :status,
            e.completedAt = :completedAt
        WHERE e.eventId = :eventId
        """)
    void updateStatus(
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
    void updateFailure(
            @Param("eventId") String eventId,
            @Param("status") ExecutionStatus status,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") LocalDateTime completedAt
    );


}
