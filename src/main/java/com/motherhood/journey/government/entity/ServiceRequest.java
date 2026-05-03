package com.motherhood.journey.government.entity;


import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "service_requests", indexes = {
        @Index(name = "idx_sr_ref", columnList = "reference_no", unique = true),
        @Index(name = "idx_sr_requester", columnList = "requester_id"),
        @Index(name = "idx_sr_facility", columnList = "facility_id"),
        @Index(name = "idx_sr_status", columnList = "status"),
        @Index(name = "idx_sr_geo", columnList = "geo_location_id"),
        @Index(name = "idx_sr_irembo", columnList = "irembo_ticket_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "service_type", nullable = false, length = 32)
    private String serviceType;

    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reference_no", nullable = false, unique = true, length = 32)
    private String referenceNo;

    @Column(name = "irembo_ticket_id", length = 64)
    private String iremboTicketId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> payload;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
}