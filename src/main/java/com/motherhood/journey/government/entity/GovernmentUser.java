package com.motherhood.journey.government.entity;

import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "government_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentUser {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "gov_role", nullable = false, length = 32)
    private String govRole;

    @Column(nullable = false, length = 32)
    private String ministry;

    @Column(name = "employee_id", nullable = false, unique = true, length = 64)
    private String employeeId;

    // Stored as UUID array in PostgreSQL
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scoped_geo_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] scopedGeoIds = new UUID[0];

    @Column(name = "can_export")
    @Builder.Default
    private Boolean canExport = false;

    @Column(name = "can_push_hmis")
    @Builder.Default
    private Boolean canPushHmis = false;

    @Column(name = "last_audit")
    private LocalDateTime lastAudit;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}