package com.motherhood.journey.appointment.entity;

import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_patient", columnList = "patient_ref_id"),
        @Index(name = "idx_appt_facility", columnList = "facility_id"),
        @Index(name = "idx_appt_datetime", columnList = "scheduled_at"),
        @Index(name = "idx_appt_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_worker_id")
    private User healthWorker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id")
    private GeoLocation geoLocation;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "appointment_type", nullable = false, length = 32)
    private String appointmentType;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}