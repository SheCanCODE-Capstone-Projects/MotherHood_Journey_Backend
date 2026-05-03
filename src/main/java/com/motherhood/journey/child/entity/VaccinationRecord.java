package com.motherhood.journey.child.entity;

import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccination_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_child_schedule", columnNames = {"child_id", "schedule_id"}),
        indexes = {
                @Index(name = "idx_vacc_rec_child", columnList = "child_id"),
                @Index(name = "idx_vacc_rec_status", columnList = "status"),
                @Index(name = "idx_vacc_rec_due", columnList = "due_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationRecord {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private VaccinationSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administered_by")
    private User administeredBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "lot_number", length = 32)
    private String lotNumber;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}