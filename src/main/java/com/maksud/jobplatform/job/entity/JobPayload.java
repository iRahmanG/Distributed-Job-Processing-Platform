package com.maksud.jobplatform.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_payloads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPayload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payload_id")
    private Long payloadId;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "job_id",
            nullable = false,
            referencedColumnName = "job_id"
    )
    private Job job;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
