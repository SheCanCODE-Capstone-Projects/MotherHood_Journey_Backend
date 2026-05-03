package com.motherhood.journey.maternal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_rx_visit", columnList = "visit_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private HealthVisit visit;

    @Column(name = "medication_name", nullable = false, length = 64)
    private String medicationName;

    @Column(nullable = false, length = 64)
    private String dosage;

    @Column(nullable = false, length = 64)
    private String frequency;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}