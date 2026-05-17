package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.facility
            WHERE a.scheduledAt BETWEEN :from AND :to
            AND a.reminderSent = false
            """)
    List<Appointment> findUpcomingUnreminded(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<Appointment> findByPatientRefIdOrderByScheduledAtDesc(UUID patientRefId);
}