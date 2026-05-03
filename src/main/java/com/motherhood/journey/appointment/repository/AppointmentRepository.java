package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
