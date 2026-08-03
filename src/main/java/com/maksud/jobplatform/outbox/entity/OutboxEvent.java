package com.maksud.jobplatform.outbox.entity;

import com.maksud.jobplatform.outbox.enums.EventType;
import com.maksud.jobplatform.outbox.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name ="outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "event_id", length = 26)
    private String eventId;

    @Column(name = "job_id", nullable = false, length = 26)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name= "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
