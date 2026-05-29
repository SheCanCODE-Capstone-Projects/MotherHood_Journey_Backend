package com.motherhood.journey.maternal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pregnancies")
@SQLDelete(sql = "UPDATE pregnancies SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pregnancy {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Fk to mothers table
    @Column(name = "mother_id", nullable = false)
    private UUID motherId;

    @Column(name = "lmp_date")
    private LocalDate lmpDate;

    // LMP + 280 days
    @Column(name = "edd")
    private LocalDate edd;

    // Active ,Delivered , Lost , Transferred

    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    // Total number of pregnancies
    @Column(name = "gravida")
    private Integer gravida;

    // Number of previous live births
    @Column(name = "para")
    private Integer para;

    // Fk to users table
    @Column(name = "assigned_chw_id")
    private UUID assignedChwId;

    @Column(name = "outcome_notes", columnDefinition = "TEXT")
    private String outcomeNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}