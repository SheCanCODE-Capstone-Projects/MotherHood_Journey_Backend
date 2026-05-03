package com.motherhood.journey.maternal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagnoses", indexes = {
        @Index(name = "idx_diag_visit", columnList = "visit_id"),
        @Index(name = "idx_diag_icd10", columnList = "icd10_code")
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

    @Column(name = "icd10_code", nullable = false, length = 8)
    private String icd10Code;

    @Column(nullable = false, length = 32)
    private String description;

    @Column(length = 16)
    private String severity;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}