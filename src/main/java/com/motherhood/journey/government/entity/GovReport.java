package com.motherhood.journey.government.entity;

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
@Table(name = "gov_reports", indexes = {
        @Index(name = "idx_greport_user", columnList = "generated_by"),
        @Index(name = "idx_greport_geo", columnList = "geo_location_id"),
        @Index(name = "idx_greport_type_period", columnList = "report_type, period, scope_level"),
        @Index(name = "idx_greport_hmis", columnList = "hmis_push_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovReport {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geo_location_id", nullable = false)
    private GeoLocation geoLocation;

    @Column(name = "report_type", nullable = false, length = 32)
    private String reportType;

    @Column(nullable = false, length = 16)
    private String period;

    @Column(name = "scope_level", nullable = false, length = 16)
    private String scopeLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> aggregates;

    @Column(name = "hmis_push_status", length = 16)
    @Builder.Default
    private String hmisPushStatus = "NOT_PUSHED";

    @Column(name = "generated_at", updatable = false)
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;
}