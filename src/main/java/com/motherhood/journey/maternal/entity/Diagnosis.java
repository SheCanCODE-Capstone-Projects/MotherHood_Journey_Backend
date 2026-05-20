package com.motherhood.journey.maternal.entity;

import com.motherhood.journey.maternal.enums.DiagnosisSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagnoses", indexes = {
        @Index(name = "idx_diag_visit",  columnList = "visit_id"),
        @Index(name = "idx_diag_icd10",  columnList = "icd10_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private HealthVisit visit;

    /**
     * ICD-10 code — validated against the seeded MoH HMIS subset
     * in the service layer before persisting.
     */
    @Column(name = "icd10_code", nullable = false, length = 8)
    private String icd10Code;

    @Column(nullable = false, length = 255)
    private String description;

    /**
     * MILD | MODERATE | SEVERE
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private DiagnosisSeverity severity;

    /**
     * Exactly one diagnosis per visit should be flagged true for
     * analytics aggregation. Enforced in the service layer.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
