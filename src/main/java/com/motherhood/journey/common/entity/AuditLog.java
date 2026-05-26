package com.motherhood.journey.common.entity;

import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_user",        columnList = "user_id"),
                @Index(name = "idx_audit_resource",    columnList = "resource_type"),
                @Index(name = "idx_audit_resource_id", columnList = "resource_type,resource_id"),
                @Index(name = "idx_audit_ts",          columnList = "created_at")
        }
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "geo_location_id")
    private UUID geoLocationId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "success", nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "fail_reason", length = 256)
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();


}