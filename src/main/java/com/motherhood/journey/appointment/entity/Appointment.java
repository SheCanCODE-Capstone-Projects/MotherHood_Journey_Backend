package com.motherhood.journey.appointment.entity;

import com.motherhood.journey.appointment.enums.AppointmentStatus;
import com.motherhood.journey.appointment.enums.AppointmentType;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_patient",  columnList = "patient_ref_id"),
        @Index(name = "idx_appt_facility", columnList = "facility_id"),
        @Index(name = "idx_appt_datetime", columnList = "scheduled_at"),
        @Index(name = "idx_appt_status",   columnList = "status")
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

    /** Polymorphic reference — points to Mother or Child id. */
    @Column(name = "patient_ref_id", nullable = false, columnDefinition = "UUID")
    private UUID patientRefId;

    /** MOTHER or CHILD */
    @Column(name = "patient_type", nullable = false, length = 8)
    private String patientType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false, length = 32)
    private AppointmentType appointmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Populated when status transitions to CANCELLED. */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
