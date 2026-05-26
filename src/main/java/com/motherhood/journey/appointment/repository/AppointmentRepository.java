package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import com.motherhood.journey.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByFacility_Id(UUID facilityId);

    Optional<Appointment> findByIdAndFacility_Id(UUID id, UUID facilityId);

    List<Appointment> findByPatientRefIdAndPatientTypeAndFacility_Id(
        UUID patientRefId, String patientType, UUID facilityId);

    List<Appointment> findByFacility_IdAndStatus(UUID facilityId, AppointmentStatus status);

    long countByStatus(AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.facility.id = :facilityId " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.scheduledAt >= :slotStart AND a.scheduledAt < :slotEnd")
    long countScheduledInSlot(@Param("facilityId") UUID facilityId,
                              @Param("slotStart") LocalDateTime slotStart,
                              @Param("slotEnd") LocalDateTime slotEnd);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patientRefId = :patientRefId " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.scheduledAt >= :slotStart AND a.scheduledAt < :slotEnd")
    long countOverlappingForPatient(@Param("patientRefId") UUID patientRefId,
                                    @Param("slotStart") LocalDateTime slotStart,
                                    @Param("slotEnd") LocalDateTime slotEnd);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'SCHEDULED' " +
           "AND a.reminderSent = false " +
           "AND a.scheduledAt BETWEEN :from AND :to")
    List<Appointment> findUpcomingWithoutReminder(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
