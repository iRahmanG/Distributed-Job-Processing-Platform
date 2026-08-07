package com.maksud.jobplatform.deadletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 26)
    private String jobId;

    @Column(nullable = false, length = 50)
    private String jobType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stackTrace;
}