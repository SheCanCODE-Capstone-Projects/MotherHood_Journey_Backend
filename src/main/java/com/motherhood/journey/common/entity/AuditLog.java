package com.motherhood.journey.common.entity;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "user_id")
    private UUID userId;

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


    public static AuditLog success(UUID userId, String action,
                                   String resourceType, UUID resourceId,
                                   String ipAddress) {
        return AuditLog.builder()
                .userId(userId).action(action)
                .resourceType(resourceType).resourceId(resourceId)
                .ipAddress(ipAddress).success(true).build();
    }

    public static AuditLog failure(UUID userId, String action,
                                   String resourceType, String failReason,
                                   String ipAddress, String userAgent) {
        return AuditLog.builder()
                .userId(userId).action(action)
                .resourceType(resourceType)
                .ipAddress(ipAddress).userAgent(userAgent)
                .success(false).failReason(failReason).build();
    }
}