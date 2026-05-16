package com.motherhood.journey.child.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccination_schedules", indexes = {
        @Index(name = "idx_vacc_sched_code", columnList = "antigen_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationSchedule {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "vaccine_name", nullable = false, length = 64)
    private String vaccineName;

    @Column(name = "antigen_code", nullable = false, unique = true, length = 16)
    private String antigenCode;

    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber;

    @Column(name = "due_age_days", nullable = false)
    private Integer dueAgeDays;

    @Column(name = "window_days")
    @Builder.Default
    private Integer windowDays = 7;

    @Column(name = "is_mandatory")
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}