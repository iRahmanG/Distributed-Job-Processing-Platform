package com.maksud.jobplatform.job.entity;

import com.maksud.jobplatform.job.entity.enums.JobPriority;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Job {
    @Id
    @Column(name = "job_id", nullable = false, length = 26)
    private String jobId;

    @Column(nullable = false,length = 50)
    private String jobType;

    @Column(nullable = false,name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(nullable = false, name = "priority")
    @Enumerated(EnumType.STRING)
    private JobPriority priority;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
