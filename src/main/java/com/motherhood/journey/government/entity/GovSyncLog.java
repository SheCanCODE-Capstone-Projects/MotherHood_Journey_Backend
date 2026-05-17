package com.motherhood.journey.government.entity;

import com.motherhood.journey.geo.entity.Facility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gov_sync_log", indexes = {
        @Index(name = "idx_gsync_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_gsync_status",      columnList = "status"),
        @Index(name = "idx_gsync_target",      columnList = "target_system"),
        @Index(name = "idx_gsync_retry",       columnList = "next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovSyncLog {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "target_system", nullable = false, length = 16)
    private String targetSystem;

    @Column(name = "sync_type", nullable = false, length = 32)
    private String syncType;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}