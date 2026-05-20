package com.motherhood.identity.domain.entity;

import jakarta.persistence.*;
import lombok.*;
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
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "gov_role", nullable = false, length = 32)
    private String govRole;

    @Column(nullable = false, length = 128)
    private String ministry;

    @Column(name = "employee_id", nullable = false, unique = true, length = 64)
    private String employeeId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scoped_geo_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] scopedGeoIds = new UUID[0];

    @Column(name = "can_export", nullable = false)
    @Builder.Default
    private boolean canExport = false;

    @Column(name = "can_push_hmis", nullable = false)
    @Builder.Default
    private boolean canPushHmis = false;

    @Column(name = "last_audit")
    private LocalDateTime lastAudit;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
