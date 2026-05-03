package com.motherhood.journey.maternal.entity;

import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pregnancies", indexes = {
        @Index(name = "idx_pregnancy_mother", columnList = "mother_id"),
        @Index(name = "idx_pregnancy_chw", columnList = "assigned_chw_id"),
        @Index(name = "idx_pregnancy_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pregnancy {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mother_id", nullable = false)
    private Mother mother;

    @Column(name = "lmp_date")
    private LocalDate lmpDate;

    @Column(name = "edd")
    private LocalDate edd;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    private Integer gravida;

    private Integer para;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_chw_id")
    private User assignedChw;

    @Column(name = "outcome_notes", columnDefinition = "TEXT")
    private String outcomeNotes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}