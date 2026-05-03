package com.motherhood.journey.government.entity;

import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

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
    @Column(name = "scoped_geo_ids", columnDefinition = "UUID[]")
    private UUID[] scopedGeoIds;

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