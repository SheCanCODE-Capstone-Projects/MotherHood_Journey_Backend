package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import com.motherhood.journey.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientRefId(UUID patientRefId);

    List<Appointment> findByFacilityIdAndStatus(UUID facilityId, AppointmentStatus status);

    /** Capacity check — count scheduled appointments at a facility within a time window. */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.facility.id = :facilityId
            AND a.status = 'SCHEDULED'
            AND a.scheduledAt BETWEEN :from AND :to
            """)
    long countScheduledInWindow(
            @Param("facilityId") UUID facilityId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Appointment> findByFacilityIdAndScheduledAtBetween(
            UUID facilityId, LocalDateTime from, LocalDateTime to);
}
