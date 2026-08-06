package com.maksud.jobplatform.deadletter.entity;

import com.maksud.jobplatform.job.entity.JobPayload;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_dlq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterJob {
    @Id
    private String id;

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false, length = 50)
    private String jobType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;
}
