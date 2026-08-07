package com.maksud.jobplatform.job.entity;

import com.maksud.jobplatform.job.entity.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_executions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_execution_event",
                        columnNames = "event_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @Column(length = 26, nullable = false)
    private String id;

    @Column(name = "job_id", length = 26, nullable = false)
    private String jobId;

    @Column(name = "event_id", length = 26, nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}