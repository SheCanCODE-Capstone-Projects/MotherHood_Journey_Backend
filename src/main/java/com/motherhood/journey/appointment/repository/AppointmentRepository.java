package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
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

    List<Appointment> findByFacility_IdAndStatus(UUID facilityId, String status);

    long countByStatus(String status);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'SCHEDULED' " +
           "AND a.reminderSent = false " +
           "AND a.scheduledAt BETWEEN :from AND :to")
    List<Appointment> findUpcomingWithoutReminder(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
