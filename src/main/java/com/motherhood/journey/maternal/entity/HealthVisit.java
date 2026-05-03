package com.motherhood.journey.maternal.entity;


import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_visits", indexes = {
        @Index(name = "idx_visit_patient", columnList = "patient_ref_id"),
        @Index(name = "idx_visit_facility", columnList = "facility_id"),
        @Index(name = "idx_visit_datetime", columnList = "visit_datetime"),
        @Index(name = "idx_visit_worker", columnList = "health_worker_id"),
        @Index(name = "idx_visit_patient_poly", columnList = "patient_ref_id, patient_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthVisit {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    // Polymorphic reference — can point to Mother or Child
    @Column(name = "patient_ref_id", nullable = false, columnDefinition = "UUID")
    private UUID patientRefId;

    @Column(name = "patient_type", nullable = false, length = 8)
    private String patientType; // "MOTHER" or "CHILD"

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "health_worker_id", nullable = false)
    private User healthWorker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id")
    private GeoLocation geoLocation;

    @Column(name = "visit_datetime", nullable = false)
    private LocalDateTime visitDatetime;

    @Column(name = "visit_type", nullable = false, length = 16)
    private String visitType;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "muac_cm")
    private Double muacCm;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}